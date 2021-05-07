package ch.psi.pshell.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import ch.psi.utils.Configurable;
import ch.psi.utils.Chrono;
import ch.psi.utils.Condition;
import ch.psi.utils.IO;
import ch.psi.utils.ProcessFactory;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.treewalk.FileTreeIterator;

/**
 * Management of embedded GIT repository, versioning the following folders: script, plugin, config
 * and devices.
 */
public class VersioningManager implements AutoCloseable {

    public static final String MASTER_BRANCH = "master";
    public static final String DEFAULT_SERVER_NAME = "origin";
    public static final String LOCAL_BRANCHES_PREFIX = "refs/heads/";
    public static final String REMOTE_BRANCHES_PREFIX = "refs/remotes/" + DEFAULT_SERVER_NAME + "/"; //assumign creating remote with defautl name: origin
    public static final String LOCAL_TAGS_PREFIX = "refs/tags/";

    final String localPath;
    final String remotePath;
    final String remoteLogin;
    final String privateKeyFile;
    final Context context;
    final boolean manualCommit;

    Repository localRepo;
    Git git;

    String configPath;
    String devicePath;
    String scriptPath;
    String pluginsPath;

    static final Logger logger = Logger.getLogger(VersioningManager.class.getName());

    public VersioningManager() {
        logger.info("Initializing " + getClass().getSimpleName());
        context = Context.getInstance();

        manualCommit = context.isVersioningManual();
        localPath = context.getSetup().getHomePath();
        remotePath = context.getConfig().versionTrackingRemote.trim();
        remoteLogin = context.getConfig().versionTrackingLogin.trim();

        if (getConnectionType() == ConnectionType.ssh) {
            //Evaluating here in order startPush to work in different process (no context)
            String secret = getKeyFileSecret(remoteLogin);
            if (secret == null) {
                File secretFile = new File(context.getSetup().expandPath("{context}/secret"));
                if (secretFile.exists()) {
                    try {
                        secret = new String(Files.readAllBytes(secretFile.toPath())).trim();
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, null, ex);
                    }
                }
            }
            privateKeyFile = (secret == null)
                    ? context.getSetup().expandPath(remoteLogin)
                    : context.getSetup().expandPath(remoteLogin) + ":" + secret;
        } else {
            privateKeyFile = null;
        }        

        if (!manualCommit) {
            context.addListener(contextListener);
        }
        try {
            //Lock file check
            Path lockFile = Paths.get(localPath, ".git", "index.lock");
            Chrono chrono = new Chrono();

            //Give extra 10s to lock file disappear because a push upstream may be going on
            if (lockFile.toFile().exists()) {
                logger.warning("Waiting lock file");
            }
            try {
                chrono.waitCondition(new Condition() {
                    @Override
                    public boolean evaluate() throws InterruptedException {
                        return !lockFile.toFile().exists();
                    }
                }, 10000, 100);
            } catch (TimeoutException ex) {
            }

            if (lockFile.toFile().exists()) {
                logger.warning("Cleaning lock file: " + lockFile.toString());
                try {
                    if (!lockFile.toFile().delete()) {
                        logger.severe("Cannot delete lock file: " + lockFile.toString());
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }

            //Initialization
            boolean createNewRepo = createLocalRepo();
            git = new Git(localRepo);
            if (IO.isSubPath(context.getSetup().getDevicesPath(), localPath)) {
                devicePath = IO.getRelativePath(context.getSetup().getDevicesPath(), localPath);
            }
            if (IO.isSubPath(context.getSetup().getScriptPath(), localPath)) {
                scriptPath = IO.getRelativePath(context.getSetup().getScriptPath(), localPath);
            }
            if (IO.isSubPath(context.getSetup().getConfigPath(), localPath)) {
                configPath = IO.getRelativePath(context.getSetup().getConfigPath(), localPath);
            }
            if (IO.isSubPath(context.getSetup().getPluginsPath(), localPath)) {
                pluginsPath = IO.getRelativePath(context.getSetup().getPluginsPath(), localPath);
            }

            if (createNewRepo) {
                add(".gitignore");
                if (!manualCommit) {
                    startCommitAll("Creation");
                }
            } else if (!manualCommit) {
                List<DiffEntry> diffs = diff();
                if ((diffs.size() > 0)) {
                    logger.info("Working tree has changes: " + diffs.size());
                    startCommitAll("Startup");
                } else {
                    logger.info("Working tree has no change");
                }
            }

            if (remotePath != null) {
                StoredConfig config = git.getRepository().getConfig();
                config.setString("remote", "origin", "url", remotePath);
                config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                config.save();
                //startPush();
            }
            //watchService = FileSystems.getDefault().newWatchService();
            //registerFolderEvents(Paths.get(localPath, "devices"));
            logger.info("Finished " + getClass().getSimpleName() + " initialization");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    enum ConnectionType {

        http,
        ssh
    }

    File getKeyFile() {
        return getKeyFile(privateKeyFile);
    }

    File getKeyFile(String str) {
        if (str != null) {
            String name = str;
            //Is secret included?
            if (name.contains(":")) {
                String file = name.substring(0, name.lastIndexOf(":"));
                File ret = new File(file);
                if (ret.exists()) {
                    return ret;
                }
            }
            File ret = new File(name);
            if (ret.exists() && ret.isFile()) {
                return ret;
            }
        }
        return null;
    }

    String getKeyFileSecret() {
        return getKeyFileSecret(privateKeyFile);
    }

    String getKeyFileSecret(String str) {
        if (str != null) {
            if (str.contains(":")) {
                File f = new File(str.substring(0, str.lastIndexOf(":")));
                if (f.exists()) {
                    String secret = str.substring(str.lastIndexOf(":") + 1).trim();
                    return secret;
                }
            }
        }
        return null;
    }

    boolean requiresPassword() {
        if (getConnectionType() == ConnectionType.ssh) {
            if (!isKeyFileEncripted() || (getKeyFileSecret() != null)) {
                return false;
            }
        }
        return (remoteLogin != null) && (!remoteLogin.contains(":"));
    }

    boolean isKeyFileEncripted() {
        try {
            File keyFile = getKeyFile();
            if (keyFile != null) {
                return new String(Files.readAllBytes(keyFile.toPath())).contains("ENCRYPTED");
            }
        } catch (IOException ex) {
        }
        return false;
    }

    ConnectionType getConnectionType() {
        if (remotePath == null) {
            return null;
        }
        if (remotePath.startsWith("http")) {
            return ConnectionType.http;
        }
        return ConnectionType.ssh;
    }

    //Constructor for process factory
    VersioningManager(String localPath, String remotePath, String remoteLogin, String keyFile) throws Exception {
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.remoteLogin = remoteLogin;
        this.privateKeyFile = keyFile;
        this.context = null;
        this.manualCommit = true;

        File gitFolder = Paths.get(localPath, ".git").toFile();
        localRepo = new FileRepository(gitFolder);
        if (!existsLocalRepo()) {
            throw new Exception("No local repo: " + gitFolder.getPath());
        }
        git = new Git(localRepo);

    }

    void logException(Exception ex) {
        logger.log(Level.WARNING, null, ex);
    }

    final boolean existsLocalRepo() {
        return new File(localPath, ".git").isDirectory();
    }

    final boolean createLocalRepo() {
        try {
            localRepo = new FileRepository(Paths.get(localPath, ".git").toFile());

            if (!existsLocalRepo()) {
                //TODO: try to pull, else create                

                createIgnore();
                localRepo.create();
                return true;
            }
        } catch (Exception ex) {
            logException(ex);
        }
        return false;
    }

    void createIgnore() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("/*\n");
            sb.append("!/script\n");
            sb.append("!/plugins\n");
            sb.append("!/devices\n");
            sb.append("!/config\n");
            sb.append("script/Lib\n");
            sb.append("script/**/cachedir\n");
            sb.append("script/**/*.class\n");
            sb.append("script/**/*.pyc\n");
            sb.append("script/**/*.*~\n");
            sb.append("plugins/*.class\n");
            sb.append("plugins/**/*.*~\n");
            sb.append("config/**/*.*~\n");
            sb.append("devices/**/*.*~\n");
            Files.write(Paths.get(localPath, ".gitignore"), sb.toString().getBytes());
        } catch (Exception ex) {
            logException(ex);
        }
    }

    void cloneRepo() {
        try {
            Git.cloneRepository().setURI(remotePath).setDirectory(new File(localPath)).call();
        } catch (Exception ex) {
            logException(ex);
        }
    }

    void addHomeFolders() {
        if (devicePath != null) {
            try {
                add(devicePath);
            } catch (Exception ex) {
                logException(ex);
            }
        }
        if (scriptPath != null) {
            try {
                add(scriptPath);
            } catch (Exception ex) {
                logException(ex);
            }
        }
        if (configPath != null) {
            try {
                add(configPath);
            } catch (Exception ex) {
                logException(ex);
            }
        }
        if (pluginsPath != null) {
            try {
                add(pluginsPath);
            } catch (Exception ex) {
                logException(ex);
            }
        }

    }

    void commit(String message) {
        try {
            logger.info("Commit: " + message);
            git.commit().setMessage(message).call();
        } catch (Exception ex) {
            logException(ex);
        }
    }

    void commitAll(String message) {
        try {
            logger.info("Commit all: " + message);
            git.commit().setAll(true).setMessage(message).call();
        } catch (Exception ex) {
            logException(ex);
        }
    }

    void push(boolean allBranches, boolean force) {
        try {
            logger.info("Push: allBranches=" + allBranches + " force=" + force);
            pushToUpstream(allBranches, force);
        } catch (Exception ex) {
            logException(ex);
        }
    }

    void trackMaster() {
        trackBranch(MASTER_BRANCH);
    }

    void trackBranch(String branch) {
        try {
            if (hasRemoteRepo()) {
                logger.info("Branch create: " + branch);
                git.branchCreate().setName(branch)
                        .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                        .setStartPoint(DEFAULT_SERVER_NAME + "/" + branch).setForce(true).call();
            }
        } catch (Exception ex) {
            logException(ex);
        }
    }

    void cleanupRepository() throws Exception {
        try {
            logger.info("Cleanup repository");
            Properties p = git.gc().call();
        } catch (Exception ex) {
            logException(ex);
            throw ex;
        }
    }

    List<DiffEntry> diff(ObjectId oldRevision, ObjectId newRevision) {
        try {
            ObjectReader reader = localRepo.newObjectReader();
            CanonicalTreeParser parser1 = new CanonicalTreeParser();
            parser1.reset(reader, oldRevision);
            CanonicalTreeParser parser2 = new CanonicalTreeParser();
            parser2.reset(reader, newRevision);
            List<DiffEntry> diffs = git.diff()
                    .setNewTree(parser2)
                    .setOldTree(parser1)
                    //.setCached(true);//Only staged
                    .call();
            return diffs;
        } catch (Exception ex) {
            logException(ex);
            return new ArrayList<>();
        }
    }

    List<DiffEntry> diff() {
        try {
            //#This eventually fails returning empty
            //List<DiffEntry> diffs = git.diff().call();

            //AbstractTreeIterator parser1 = prepareTreeParser( Constants.HEAD );            
            //List<DiffEntry> diffs = git.diff().setOldTree(parser1).setNewTree(parser2).call();
            //Faster
            DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream());
            formatter.setRepository(localRepo);
            AbstractTreeIterator commitTreeIterator = prepareTreeParser(Constants.HEAD);
            FileTreeIterator workTreeIterator = new FileTreeIterator(localRepo);
            List<DiffEntry> diffs = formatter.scan(commitTreeIterator, workTreeIterator);
            return diffs;
        } catch (Exception ex) {
            logException(ex);
            return new ArrayList<>();
        }
    }

    List<DiffEntry> diff(String file, ObjectId oldRevision, ObjectId newRevision) {

        try {
            ObjectReader reader = localRepo.newObjectReader();
            CanonicalTreeParser parser1 = new CanonicalTreeParser();
            parser1.reset(reader, oldRevision);
            CanonicalTreeParser parser2 = new CanonicalTreeParser();
            parser2.reset(reader, newRevision);
            List<DiffEntry> diffs = git.diff().setPathFilter(PathFilter.create(file))
                    .setNewTree(parser2)
                    .setOldTree(parser1)
                    .call();
            return diffs;
        } catch (Exception ex) {
            logException(ex);
            return new ArrayList<>();
        }
    }

    List<DiffEntry> diffFile(String file) {

        try {
            List<DiffEntry> diffs = git.diff().setPathFilter(PathFilter.create(file)).call();
            return diffs;
        } catch (Exception ex) {
            logException(ex);
            return new ArrayList<>();
        }
    }

    AbstractTreeIterator prepareTreeParser(String objectId) throws Exception {
        //Current files
        if (objectId == null) {
            return new FileTreeIterator(localRepo);
        }
        RevWalk walk = new RevWalk(localRepo);
        RevCommit commit = null;
        try {
            commit = walk.parseCommit(ObjectId.fromString(objectId));
        } catch (Exception ex) {
            commit = walk.parseCommit(localRepo.getRef(objectId).getObjectId());
        }
        RevTree tree = walk.parseTree(commit.getTree().getId());

        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        ObjectReader oldReader = localRepo.newObjectReader();
        try {
            oldTreeParser.reset(oldReader, tree.getId());
        } finally {
            oldReader.close();
        }

        walk.dispose();

        return oldTreeParser;
    }

    String diffFile(String path, String oldRevision, String newRevision) throws Exception {
        try (OutputStream out = new ByteArrayOutputStream()) {
            AbstractTreeIterator oldTreeParser = prepareTreeParser(oldRevision);
            AbstractTreeIterator newTreeParser = prepareTreeParser(newRevision);

            List<DiffEntry> diff = git.diff().
                    setOldTree(oldTreeParser).
                    setNewTree(newTreeParser).
                    setPathFilter(PathFilter.create(path)).
                    setOutputStream(out).
                    call();
            return out.toString();
        }
    }

    ObjectId getHeadTree() throws IOException {
        return localRepo.resolve("HEAD^{tree}");
    }

    ObjectId getTree(String revision) throws IOException {
        return localRepo.resolve(revision + "^{tree}");
    }

    String getHeadRevisionNumber() throws Exception {
        ArrayList<Revision> ret = new ArrayList<>();
        Iterable<RevCommit> logs = git.log().call();

        for (RevCommit log : logs) {
            return log.getName();
        }
        return null;
    }

    final ContextListener contextListener = new ContextAdapter() {
        @Override
        public void onExecutingFile(String fileName) {
            if ((scriptPath != null) && (fileName != null)) {
                if (IO.isSubPath(fileName, localPath)) {

                    String relPath = IO.getRelativePath(fileName, localPath);
                    //relPath.replaceAll("/", "\\");
                    startCheckCommitFile(relPath, "Script execution");
                }
            }
        }

        @Override
        public void onConfigurationChange(Configurable obj) {
            if ((devicePath != null) && (obj != null) && (obj.getConfig() != null)) {
                String fileName = obj.getConfig().getFileName();
                if (fileName != null) {
                    String relPath = IO.getRelativePath(fileName, localPath);
                    startCheckCommitFile(relPath, "Configuration change: " + obj.toString());
                }
            }
        }
    };

    final ForkJoinPool executorService = new ForkJoinPool(1, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    void startCheckCommitFile(final String fileName, final String message) {
        executorService.execute(() -> {
            List<DiffEntry> diff = diffFile(fileName);
            for (DiffEntry entry : diff) {
                if (entry.getNewPath().equals(fileName)) {
                    //if ((entry.getChangeType()==ChangeType.MODIFY)||(entry.getChangeType()==ChangeType.ADD)||(entry.getChangeType()==ChangeType.RENAME)){
                    try {
                        add(fileName);
                    } catch (Exception ex) {
                        logException(ex);
                    }
                    commit(message);
                    break;
                    //}
                }
            }
        });
    }

    public void startCommitAll(final String message) {
        executorService.execute(() -> {
            addHomeFolders();
            commitAll(message);
        });
    }

    void startPush(final boolean allBranches, final boolean force) {
        executorService.execute(() -> {
            push(allBranches, force);
        });
    }

    @Override
    public void close() {
        //checkFolderEvents();
        if (context != null) {
            context.removeListener(contextListener);
        }
        executorService.shutdownNow();

        if ((context != null) && (!manualCommit)) {
            if (diff().size() > 0) {
                commitAll("Closedown");
            }
        }

        try {
            logger.info("Close");
            git.close();
        } catch (Exception ex) {
            logException(ex);
        }
    }

    TransportConfigCallback getTransportConfigCallback() {
        if (getConnectionType() != ConnectionType.ssh) {
            return null;
        }
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(Host host, Session session) {
                // do nothing 
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
            }

            @Override
            protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
                JSch jsch = super.getJSch(hc, fs);
                jsch.removeAllIdentity();
                try {
                    String id = getKeyFile().getAbsolutePath();
                    if (isKeyFileEncripted()) {
                        jsch.addIdentity(id, passphrase);
                    } else {
                        jsch.addIdentity(id);
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, null, ex);
                }
                return jsch;
            }

        };
        TransportConfigCallback transportConfigCallback = (Transport transport) -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        };
        return transportConfigCallback;
    }
    String passphrase;

    CredentialsProvider getCredentialsProvider() throws Exception {
        String usr = "";
        String pwd = "";
        passphrase = null;
        CredentialsProvider credentialsProvider = null;
        if (remoteLogin != null) {
            if (requiresPassword()) {
                if ((remoteLogin != null) && (remoteLogin.length() > 0)) {
                    usr = remoteLogin;
                } else {
                    usr = context.getString("Enter user name:", "", null);
                    if (usr == null) {
                        throw new IOException("Canceled");
                    }
                }
                pwd = context.getPassword("Enter password:", "Authentication");
                if (pwd == null) {
                    throw new IOException("Canceled");
                }
            } else {
                if (remoteLogin.contains(":")) {
                    usr = remoteLogin.substring(0, remoteLogin.lastIndexOf(":")).trim();
                    pwd = remoteLogin.substring(remoteLogin.lastIndexOf(":") + 1).trim();
                } else {
                    usr = remoteLogin;
                }
            }

            if (getConnectionType() == ConnectionType.ssh) {
                usr = getKeyFile().getAbsolutePath();
                if (usr == null) {
                    throw new IOException("Invalid key file");
                }
                passphrase = requiresPassword() ? pwd : getKeyFileSecret();
                pwd = "";
            }

            credentialsProvider = new UsernamePasswordCredentialsProvider(usr, pwd);
        }

        if (usr.length() == 0) {
            credentialsProvider = CredentialsProvider.getDefault();
        }
        return credentialsProvider;
    }
    //Public interface

    /**
     * Force to overwrite remote history
     */
    public boolean hasRemoteRepo() {
        return ((remotePath != null) && (!remotePath.trim().isEmpty()));
    }

    public void pushToUpstream(final boolean allBranches, final boolean force) throws Exception {
        pushToUpstream(allBranches, force, false);
    }
    
    public void pushToUpstream(final boolean allBranches, final boolean force, final boolean pushTags) throws Exception {
        if (hasRemoteRepo()) {
            logger.info("Push to upstream all=" + allBranches + " force=" + force + " tags=" + pushTags);
            CredentialsProvider credentialsProvider = getCredentialsProvider();
            TransportConfigCallback transportConfigCallback = getTransportConfigCallback();
            PushCommand cmd = git.push().setForce(force).setRemote(remotePath);
            if (transportConfigCallback != null) {
                cmd.setTransportConfigCallback(transportConfigCallback);
            } else {
                cmd.setCredentialsProvider(credentialsProvider);
            }
            if (allBranches) {
                cmd.setPushAll();
            }
            if (pushTags){
                cmd.setPushTags();
            }
            String message = "";
            Iterable<PushResult> ret = cmd.call();
            for (PushResult res : ret) {
                message += res.getMessages() + "\n";
            }
            if (message.trim().length() > 0) {
                throw new Exception(message);
            }
        }
    }

    public void pullFromUpstream() throws Exception {
        if (hasRemoteRepo()) {
            logger.info("Pull from upstream");
            CredentialsProvider credentialsProvider = getCredentialsProvider();
            PullCommand cmd = git.pull();
            TransportConfigCallback transportConfigCallback = getTransportConfigCallback();
            if (transportConfigCallback != null) {
                cmd.setTransportConfigCallback(transportConfigCallback);
            } else {
                cmd.setCredentialsProvider(credentialsProvider);
            }
            PullResult ret = cmd.call();
            if (!ret.isSuccessful()) {
                throw new Exception(ret.toString());
            }
        }
    }

    /**
     * Executes in different process so can be called when shutting down.
     */
    public void startPushUpstream(final boolean allBranches, final boolean force) {
        if ((remotePath != null) && (!remotePath.trim().isEmpty())
                && (remoteLogin != null) && (!remoteLogin.trim().isEmpty())
                && (!requiresPassword())) {
            Process p = ProcessFactory.createProcess(VersioningManager.class, new String[]{localPath, remotePath, remoteLogin,
                privateKeyFile, String.valueOf(allBranches), String.valueOf(force)});
        }
    }

    public String getCurrentBranch() throws Exception {
        return localRepo.getBranch();
    }

    public List<String> getLocalBranches() throws Exception {
        //List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call(); //Remotes
        //List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call(); //All
        List<Ref> refs = git.branchList().call(); //Locals
        ArrayList<String> ret = new ArrayList<>();
        for (Ref ref : refs) {
            String name = ref.getName();
            if (name.startsWith(LOCAL_BRANCHES_PREFIX)) {
                name = name.substring(LOCAL_BRANCHES_PREFIX.length());
            }
            ret.add(name);
        }
        return ret;
    }

    public void checkoutLocalBranch(String branch) throws Exception {
        logger.info("Checkout branch: " + branch);
        git.checkout().setName(branch).call();
    }

    public void checkoutRemoteBranch(String branch) throws Exception {
        if (hasRemoteRepo()) {
            logger.info("Checkout remote branch: " + branch);
            CredentialsProvider credentialsProvider = getCredentialsProvider();
            git.pull().setCredentialsProvider(credentialsProvider).call();
            trackBranch(branch);
            git.checkout().setName(branch).call();
        }
    }

    public void createBranch(String branch) throws Exception {
        if (getLocalBranches().contains(branch)) {
            throw new Exception("Branch " + branch + " already exists");
        }
        logger.info("Create branch: " + branch);
        git.branchCreate().setName(branch).call();
    }

    public void deleteBranch(String branch) throws Exception {
        if (MASTER_BRANCH.equals(branch)) {
            throw new Exception("Cannot delete master branch");
        }
        if (String.valueOf(getCurrentBranch()).equals(branch)) {
            throw new Exception("Cannot delete current branch");
        }
        logger.info("Delete branch: " + branch);
        git.branchDelete().setBranchNames(branch).setForce(true).call();
    }

    public List<String> getTags() throws Exception {
        List<Ref> refs = git.tagList().call(); //Locals
        ArrayList<String> ret = new ArrayList<>();
        for (Ref ref : refs) {
            String name = ref.getName();
            if (name.startsWith(LOCAL_TAGS_PREFIX)) {
                name = name.substring(LOCAL_TAGS_PREFIX.length());
            }
            ret.add(name);
        }
        return ret;
    }

    public void createTag(String name, String message) throws Exception {
        logger.info("Create tag: " + name);
        TagCommand tag = git.tag().setName(name).setMessage(message);
        if (message != null) {
            tag.setMessage(message);
        }
        tag.call();
    }

    public void deleteTag(String tag) throws Exception {
        logger.info("Delete tag: " + tag);
        git.tagDelete().setTags(tag).call();
    }

    public void checkoutTag(String tag) throws Exception {
        logger.info("Checkout tag: " + tag);
        git.checkout().setName(VersioningManager.LOCAL_TAGS_PREFIX + tag).call();
    }

    public void add(String fileName) throws Exception {
        logger.info("Adding: " + fileName);
        git.add().addFilepattern(fileName).call();
    }

    public void add(String folder, String ext) throws Exception {
        Path path = Paths.get(localPath, folder);
        for (File file : IO.listFiles(path.toString(), "*." + ext)) {
            add(Paths.get(folder, file.getName()).toString());
        }
    }

    /**
     * Entity class holding the information of a certain commit of the repository.
     */
    public static class Revision {

        public String id;
        public long timestamp;
        public String commiter;
        public String message;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(" - ");
            sb.append(Chrono.getTimeStr(timestamp, "dd/MM/YY HH:mm:ss")).append(" - ");
            sb.append(commiter).append(" - ");
            sb.append(message);
            return sb.toString();
        }
    }

    public List<Revision> getHistory(String fileName, int maxCount) throws Exception {
        ArrayList<Revision> ret = new ArrayList<>();
        LogCommand cmd = git.log().addPath(fileName);
        if (maxCount > 0) {
            cmd.setMaxCount(maxCount);
        }
        Iterable<RevCommit> logs = cmd.call();

        for (RevCommit log : logs) {
            Revision rev = new Revision();
            rev.id = log.getId().getName();
            rev.timestamp = log.getAuthorIdent().getWhen().getTime();
            rev.commiter = log.getAuthorIdent().getName();
            rev.message = log.getShortMessage();
            ret.add(rev);
        }
        return ret;
    }
    
    public Revision getRevision(String fileName) throws Exception {
        LogCommand cmd = git.log().addPath(fileName);
        cmd.setMaxCount(1);
        Iterable<RevCommit> logs = cmd.call();
        for (RevCommit log : logs) {
            Revision rev = new Revision();
            rev.id = log.getId().getName();
            rev.timestamp = log.getAuthorIdent().getWhen().getTime();
            rev.commiter = log.getAuthorIdent().getName();
            rev.message = log.getShortMessage();
            return rev;
        }
        return null;
    }

    final boolean contains(String fileName) throws Exception {
        return getHistory(fileName, 1).size() > 0;
    }

    /**
     * Changes between current and head
     */
    public String getDiff(String file) throws Exception {

        return getDiff(file, getHeadRevisionNumber(), null);
    }

    public String getDiff(String file, String oldRevision) throws Exception {

        return getDiff(file, oldRevision, this.getHeadRevisionNumber());
    }

    public String getDiff(String file, String oldRevision, String newRevision) throws Exception {
        return diffFile(file, oldRevision, newRevision);
    }

    /**
     * Fetch current file
     */
    public String fetch(String file) throws Exception {
        return fetch(file, null);
    }

    public String fetch(String file, String revision) throws Exception {

        if (revision == null) {
            try {
                return new String(Files.readAllBytes(Paths.get(localPath, file)));
            } catch (IOException ex) {
                return "";
            }
        }
        final ObjectId id = localRepo.resolve(revision);
        ObjectReader reader = localRepo.newObjectReader();
        try {
            RevWalk walk = new RevWalk(reader);
            RevCommit commit = walk.parseCommit(id);
            RevTree tree = commit.getTree();
            TreeWalk treeWalk = TreeWalk.forPath(reader, file, tree);

            if (treeWalk != null) {
                return new String(reader.open(treeWalk.getObjectId(0)).getBytes());
            } else {
                return null;
            }
        } finally {
            reader.close();
        }
    }

    /**
     * This is for executing a push in a different process with ProcessFactory
     */
    public static void main(String[] args) {
        //ProcessFactory
        try (VersioningManager vm = new VersioningManager(args[0], args[1], args[2], args[3])) {
            Boolean allBranches = Boolean.valueOf(args[4]);
            Boolean force = Boolean.valueOf(args[5]);
            vm.pushToUpstream(allBranches, false);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}
