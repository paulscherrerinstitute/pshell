package ch.psi.pshell.versioning;

import ch.psi.pshell.app.Setup;
import ch.psi.pshell.swing.DefaultUserInterface;
import ch.psi.pshell.swing.UserInterface;
import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Condition;
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.ObservableBase;
import ch.psi.pshell.utils.ProcessFactory;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig.Host;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

/**
 * Management of embedded GIT repository
 * and devices.
 */
public class VersionControl extends ObservableBase<VersionControlListener> implements AutoCloseable {
    
    static VersionControl INSTANCE;    
    
    public static VersionControl getInstance(){
        if (INSTANCE == null){
            throw new RuntimeException("Versioning Manager not instantiated.");
        }         
        return INSTANCE;
    }
    
    public static boolean hasInstance(){
        return INSTANCE!=null;
    }
    
    
    public static final String MASTER_BRANCH = "master";
    public static final String DEFAULT_SERVER_NAME = "origin";
    public static final String LOCAL_BRANCHES_PREFIX = "refs/heads/";
    public static final String REMOTE_BRANCHES_PREFIX = "refs/remotes/" + DEFAULT_SERVER_NAME + "/"; //assumign creating remote with default name: origin
    public static final String LOCAL_TAGS_PREFIX = "refs/tags/";
    

    public final VersionControlConfig config;
    final String localPath;
    final String remotePath;
    final String remoteLogin;
    final String privateKeyFile;
    final boolean autoCommit;
    final ArrayList<String> addedFolders;
    final String secret;

    Repository localRepo;
    Git git;
    
    static final Logger logger = Logger.getLogger(VersionControl.class.getName());
    
    
    public VersionControl(VersionControlConfig config) {
        INSTANCE  = this;
        logger.log(Level.INFO, "Initializing {0}", getClass().getSimpleName());        
        this.config = config;
        this.localPath = config.localPath();
        this.remotePath = config.remotePath();
        this.remoteLogin = config.remoteLogin();
        this.autoCommit = config.autoCommit();
        this.addedFolders = new ArrayList();        
        for (String folder: config.addedFolders()){
            if (IO.isSubPath(folder, localPath)) {
                this.addedFolders.add(IO.getRelativePath(folder, localPath));
            }
        }

        String secret = getKeyFileSecret(remoteLogin);
        if (secret == null) {
            File secretFile = new File(Setup.expandPath("{context}/secret"));
            if (secretFile.exists()) {
                try {
                    secret = new String(Files.readAllBytes(secretFile.toPath())).trim();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
        this.secret = secret;

        if (getConnectionType() == ConnectionType.ssh) {
            //Evaluating here in order startPush to work in different process (no context)
            privateKeyFile = (secret == null)
                    ? Setup.expandPath(remoteLogin)
                    : Setup.expandPath(remoteLogin) + ":" + secret;
        } else {
            privateKeyFile = null;
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
                logger.log(Level.WARNING, "Cleaning lock file: {0}", lockFile.toString());
                try {
                    if (!lockFile.toFile().delete()) {
                        logger.log(Level.SEVERE, "Cannot delete lock file: {0}", lockFile.toString());
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }

            //Initialization
            boolean createNewRepo = createLocalRepo();
            git = new Git(localRepo);

            if (createNewRepo) {
                add(".gitignore");
                if (autoCommit) {
                    startAddCommitAll("Creation");
                }
            } else if (autoCommit) {
                List<DiffEntry> diffs = diff();
                if ((diffs.size() > 0)) {
                    logger.log(Level.INFO, "Working tree has changes: {0}", diffs.size());
                    startAddCommitAll("Startup");
                } else {
                    logger.info("Working tree has no change");
                }
            }

            if (remotePath != null) {
                StoredConfig storedConfig = git.getRepository().getConfig();
                storedConfig.setString("remote", "origin", "url", remotePath);
                storedConfig.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                storedConfig.save();
                //startPush();
            }
            //watchService = FileSystems.getDefault().newWatchService();
            //registerFolderEvents(Paths.get(localPath, "devices"));
            logger.log(Level.INFO, "Finished {0} initialization", getClass().getSimpleName());
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
    
    UserInterface userInterface ;
    public UserInterface getUserInterface(){
        if (userInterface == null) {
            userInterface = new DefaultUserInterface();
        }
        
        return userInterface;
    }

    public void setUserInterface(UserInterface userInterface){
        this.userInterface = userInterface;
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
        return secret == null;
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
    VersionControl(String localPath, String remotePath, String remoteLogin, String keyFile, String secret) throws Exception {
        this.config = null;
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.remoteLogin = remoteLogin;
        this.privateKeyFile = keyFile;
        this.autoCommit = false;
        this.addedFolders = null;
        this.secret = secret;

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
            for (String folder: addedFolders){
                sb.append("!/" +folder + "\n");
            }
            sb.append("script/Lib\n");
            sb.append("/**/cachedir\n");
            sb.append("/**/*.class\n");
            sb.append("/**/*.pyc\n");
            sb.append("/**/*.*~\n");
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
    

    public void commit(String message) {
        try {
            logger.log(Level.INFO, "Commit: {0}", message);
            git.commit().setMessage(message).call();
        } catch (Exception ex) {
            logException(ex);
        }
    }

    public void commitAll(String message) {
        try {
            logger.log(Level.INFO, "Commit all: {0}", message);
            git.commit().setAll(true).setMessage(message).call();
        } catch (Exception ex) {
            logException(ex);
        }
    }
    
    
    public void addCommitAll(String message, boolean force) throws IOException {
        if (!force) {
            int diffs = diff().size();
            if (diffs == 0) {
                throw new IOException("Nothing to commit");
            }
        }
        if (addedFolders!=null){
            addFolders(addedFolders.toArray(new String[0]));
        }
        commitAll(message);        
    }    
    

    public void push(boolean allBranches, boolean force) {
        try {
            logger.log(Level.INFO, "Push: allBranches={0} force={1}", new Object[]{allBranches, force});
            pushToUpstream(allBranches, force);
        } catch (Exception ex) {
            logException(ex);
        }
    }

    public void trackMaster() {
        trackBranch(MASTER_BRANCH);
    }

    public void trackBranch(String branch) {
        try {
            if (hasRemoteRepo()) {
                logger.log(Level.INFO, "Branch create: {0}", branch);
                git.branchCreate().setName(branch)
                        .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                        .setStartPoint(DEFAULT_SERVER_NAME + "/" + branch).setForce(true).call();
            }
        } catch (Exception ex) {
            logException(ex);
        }
    }

    public void cleanupRepository() throws Exception {
        try {
            logger.info("Cleanup repository");
            Properties p = git.gc().call();
        } catch (Exception ex) {
            logException(ex);
            throw ex;
        }
    }

    public List<DiffEntry> diff(ObjectId oldRevision, ObjectId newRevision) {
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

    public List<DiffEntry> diff() {
        try {
            //List of unstaged changes, eventually fails returning empty
            return git.diff().call();
        } catch (Exception ex) {
            try {
                //Compare HEAD vs working tree
                DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream());
                formatter.setRepository(localRepo);
                AbstractTreeIterator commitTreeIterator = prepareTreeParser(Constants.HEAD);
                FileTreeIterator workTreeIterator = new FileTreeIterator(localRepo);
                List<DiffEntry> diffs = formatter.scan(commitTreeIterator, workTreeIterator);
                return diffs;
                
            } catch (Exception ex1) {                
            }
            logException(ex);
            return new ArrayList<>();
        }
    }

    public List<DiffEntry> diff(String file, ObjectId oldRevision, ObjectId newRevision) {

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

    public List<DiffEntry> diffFile(String file) {

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
            commit = walk.parseCommit(localRepo.exactRef(objectId).getObjectId());
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

    public String diffFile(String path, String oldRevision, String newRevision) throws Exception {
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

    public String getHeadRevisionNumber() throws Exception {
        ArrayList<Revision> ret = new ArrayList<>();
        Iterable<RevCommit> logs = git.log().call();

        for (RevCommit log : logs) {
            return log.getName();
        }
        return null;
    }

    public void autoCommit(String fileName, String message) {
        if (autoCommit){
            if (fileName != null) {
                if (IO.isSubPath(fileName, localPath)) {

                    String relPath = IO.getRelativePath(fileName, localPath);
                    //relPath.replaceAll("/", "\\");
                    startCheckCommitFile(relPath, message);
                }
            }
        }
    }

    final ForkJoinPool executorService = new ForkJoinPool(1, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public void startCheckCommitFile(final String fileName, final String message) {
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

    public void startAddCommitAll(final String message) {
        executorService.execute(() -> {
            if (addedFolders!=null){
                addFolders(addedFolders.toArray(new String[0]));
            }
            commitAll(message);
        });
    }

    public void startPush(final boolean allBranches, final boolean force) {
        executorService.execute(() -> {
            push(allBranches, force);
        });
    }

    @Override
    public void close() {
        executorService.shutdownNow();

        if (autoCommit) {
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
        if ((remoteLogin != null) && (!remoteLogin.isBlank())) {
            if (requiresPassword()) {
                if ((remoteLogin != null) && (remoteLogin.length() > 0)) {
                    usr = remoteLogin;
                } else {
                    usr = getUserInterface().getString("Enter user name:", "", null);
                    if (usr == null) {
                        throw new IOException("Canceled");
                    }
                }
                pwd = getUserInterface().getPassword("Enter password:", "Authentication");
                if (pwd == null) {
                    throw new IOException("Canceled");
                }
            } else {                
                if (remoteLogin.contains(":")) {
                    usr = remoteLogin.substring(0, remoteLogin.lastIndexOf(":")).trim();
                } else {
                    usr = remoteLogin;
                }
                pwd = secret;
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
    
    
    public void assertHasRemoteRepo()  throws IOException {
        if (!hasRemoteRepo()) {
            throw new IOException("Remote repository not enabled");
        }
    }    

    public void pushToUpstream(final boolean allBranches, final boolean force) throws Exception {
        pushToUpstream(allBranches, force, false);
    }
    
    public void pushToUpstream(final boolean allBranches, final boolean force, final boolean pushTags) throws Exception {
        if (hasRemoteRepo()) {
            logger.log(Level.INFO, "Push to upstream all={0} force={1} tags={2}", new Object[]{allBranches, force, pushTags});
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
            
            Iterable<PushResult> results = cmd.call();

            StringBuilder message = new StringBuilder();
            boolean hasError = false;
            for (PushResult result : results) {
                message.append(result.getMessages()).append("\n");

                for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                    RemoteRefUpdate.Status status = update.getStatus();
                    if (status != RemoteRefUpdate.Status.OK && status != RemoteRefUpdate.Status.UP_TO_DATE) {
                        hasError = true;
                        message.append("Failed to update ")
                               .append(update.getRemoteName())
                               .append(": ")
                               .append(status.name())
                               .append(" - ")
                               .append(update.getMessage())
                               .append("\n");
                    }
                }
            }

            String msg = message.toString().trim();
            if (hasError) {
                throw new Exception("Push failed:\n" +msg);
            } else {
                logger.log(Level.INFO, "Push successful" + (msg.isEmpty() ? "" : (":\n" +msg)));
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
            triggerCheckout(getCurrentBranch());
        }
    }

    /**
     * Executes in different process so can be called when shutting down.
     */
    public void startPushUpstream(final boolean allBranches, final boolean force) {
        if ((remotePath != null) && (!remotePath.trim().isEmpty())
                && (remoteLogin != null) && (!remoteLogin.isBlank())
                && (!requiresPassword())) {
            Process p = ProcessFactory.createProcess(VersionControl.class, new String[]{localPath, remotePath, remoteLogin,
                privateKeyFile, secret, 
                String.valueOf(allBranches), String.valueOf(force)});
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
        logger.log(Level.INFO, "Checkout branch: {0}", branch);
        git.checkout().setName(branch).call();
        triggerCheckout(branch);
    }

    public void checkoutRemoteBranch(String branch) throws Exception {
        if (hasRemoteRepo()) {
            logger.log(Level.INFO, "Checkout remote branch: {0}", branch);
            CredentialsProvider credentialsProvider = getCredentialsProvider();
            git.pull().setCredentialsProvider(credentialsProvider).call();
            trackBranch(branch);
            git.checkout().setName(branch).call();
            triggerCheckout(branch);
        }
    }

    public void createBranch(String branch) throws Exception {
        if (getLocalBranches().contains(branch)) {
            throw new Exception("Branch " + branch + " already exists");
        }
        logger.log(Level.INFO, "Create branch: {0}", branch);
        git.branchCreate().setName(branch).call();
    }

    public void deleteBranch(String branch) throws Exception {
        if (MASTER_BRANCH.equals(branch)) {
            throw new Exception("Cannot delete master branch");
        }
        if (String.valueOf(getCurrentBranch()).equals(branch)) {
            throw new Exception("Cannot delete current branch");
        }
        logger.log(Level.INFO, "Delete branch: {0}", branch);
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
        logger.log(Level.INFO, "Create tag: {0}", name);
        TagCommand tag = git.tag().setName(name).setMessage(message);
        if (message != null) {
            tag.setMessage(message);
        }
        tag.call();
    }

    public void deleteTag(String tag) throws Exception {
        logger.log(Level.INFO, "Delete tag: {0}", tag);
        git.tagDelete().setTags(tag).call();
    }

    public void checkoutTag(String tag) throws Exception {
        logger.log(Level.INFO, "Checkout tag: {0}", tag);
        git.checkout().setName(VersionControl.LOCAL_TAGS_PREFIX + tag).call();
        triggerCheckout(tag);
    }

    public void add(String fileName) throws Exception {
        logger.log(Level.INFO, "Adding: {0}", fileName);
        git.add().addFilepattern(fileName).call();
    }

    public void add(String folder, String ext) throws Exception {
        Path path = Paths.get(localPath, folder);
        for (File file : IO.listFiles(path.toString(), "*." + ext)) {
            add(Paths.get(folder, file.getName()).toString());
        }
    }
    
    protected void addFolders(String[] folders) {          
        for (String folder : folders){
            try {
                add(folder);
            } catch (Exception ex) {
                logException(ex);
            }
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

    protected void triggerCheckout(final String branchOrTag) {
        for (VersionControlListener listener : getListeners()) {
            try {
                listener.onCheckout(branchOrTag);
            } catch (Throwable ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }
    }
    
    //Static Utilities    
    public static String getFileContents(String fileName, String revisionId) throws IOException, InterruptedException {
        fileName = Setup.expandPath(fileName);
        if (IO.isSubPath(fileName, Setup.getHomePath())) {
            fileName = IO.getRelativePath(fileName, Setup.getHomePath());
            try {
                return getInstance().fetch(fileName, revisionId);
            } catch (IOException | InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
        return null;
    }

    public static Revision getFileRevision(String fileName) throws IOException, InterruptedException{
        Revision ret = null;
        fileName = Setup.expandPath(fileName);
        if (IO.isSubPath(fileName, Setup.getHomePath())) {
            fileName = IO.getRelativePath(fileName, Setup.getHomePath());
            try {
                ret = getInstance().getRevision(fileName);
                if (getInstance().getDiff(fileName).length() > 0) {
                    ret.id += " *";
                }
            } catch (IOException | InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
        return ret;
    }
    
    public static String getFileRevisionId(String fileName) throws IOException {
        if (fileName != null) {
            try{
                ch.psi.pshell.versioning.Revision rev =  getFileRevision(fileName);
                if (rev != null) {
                    return rev.id;
                }            
            } catch (Exception ex) {
            }
        }
        return null;
    }
    
        
    
    /**
     * This is for executing a push in a different process with ProcessFactory
     */
    public static void main(String[] args) throws FileNotFoundException {                
        //Debugging
        //java.io.File logFile = new File("output.log");
        //java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(logFile, false)); 
        //System.setOut(ps);
        //System.setErr(ps);
                
        try (VersionControl vc = new VersionControl(args[0], args[1], args[2], args[3], args[4])) {
            Boolean allBranches = Boolean.valueOf(args[5]);
            Boolean force = Boolean.valueOf(args[6]);
            vc .pushToUpstream(allBranches, force);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }
}
