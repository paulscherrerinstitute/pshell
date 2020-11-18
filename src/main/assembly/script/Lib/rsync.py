####################################################################################################
#  Utilities for synchronizing folders with rsync
#  On RH7  (not SL6)
#  Change permission of the account, otherwise SSH keys are not accepted:
#       ~/.ssh    from drwxr-S--- to drwx----
#       ~ :       from drwxrws--- to drwxr-s---
####################################################################################################

import sys
import os
import os.path
import shutil

from startup import exec_cmd, log

RSYNC_GENERATE_USER_KEY = True

def rsync(src, dest, key):
    #cmd = 'rsync -e "ssh -i ' + key + ' -o LogLevel=quiet" --chmod=ug=rwx --verbose --modify-window=1 --times --recursive ' + src + ' ' + dest
    #ret = exec_cmd(cmd)
    cmd = 'rsync -e "ssh -i ' + key + '" --chmod=ug=rwx --verbose --modify-window=1 --times --recursive ' + src + ' ' + dest
    ret = exec_cmd(cmd, False)
    lines = ret.split("\n")
    lines =  filter(lambda x: x != "", lines)
    if len(lines)<3:
        print "Invalid return from rsync:\n", ret
        raise Exception ("Invalid format")
    #files = lines[1:-2]
    files = []
    head,tail=os.path.split(src)
    for l in lines:
        f = os.path.join(head,l)
        if os.path.exists(f):
            files.append(f)
    try:
        stats = lines[-2].replace(",", "").replace(".", "")
        stats = [int(s) for s in stats.split() if s.isdigit()]
        bytes_sent, bytes_received = stats[0], stats[1]
    except:
        print "Invalid statistics from rsync:\n", ret
        bytes_sent, bytes_received = None, None

    return files, bytes_sent, bytes_received

def sync_user_data(user, src, dest, host= "localhost", remove_local_folder=False, remove_local_files=False, do_log=True, do_print=True):
    try:
        if do_log:
            log("Start synchronizing %s to %s:%s" % (src, user, dest), False )
        key = os.path.expanduser("~/.ssh/" + ("ke" if RSYNC_GENERATE_USER_KEY else "id_rsa"))
        if not os.path.isfile(key):
            raise Exception ("Invalid key file")
        dest = "'" + dest.replace(" ", "\ ") + "'"
        dest = user + "@" + host + ":" + dest
        files, bytes_sent, bytes_received = rsync(src,dest,key)
        msg = "Transferred " + str(bytes_sent) + " bytes to " + user + ": "
        for f in files:
            msg = msg + "\n" + f
        if do_log:
            log(msg, False)
        if do_print:
            print msg
        if remove_local_folder:
            if do_log:
                log("Removing folder: " + src)
            shutil.rmtree(src)
        elif remove_local_files:
            for f in files:
                if not os.path.samefile(f, src):
                    if os.path.isfile(f):
                        if do_log:
                            log("Removing file: " + f)
                        os.remove(f)
                    elif os.path.isdir(f):
                        if do_log:
                            log("Removing folder: " + f)
                        shutil.rmtree(f)

    except:
        msg = "Error transferring user data to " + user + ": " + str(sys.exc_info()[1])
        if do_log:
            log(msg, False)
        if do_print:
            print >> sys.stderr, msg
    return msg

def remove_user_key(do_print=True):
    cmd = "rm ~/.ssh/ke;"
    cmd = cmd + "rm ~/.ssh/ke.pub"
    if do_print:
        print exec_cmd(cmd, False)

def reset_user_key(do_print=True):
    remove_user_key(do_print)
    cmd = "ssh-keygen -N '' -f ~/.ssh/ke -t rsa;"
    if do_print:
        print exec_cmd(cmd)

def authorize_user(user, aux_file = os.path.expanduser("~/.rsync.tmp"), fix_permissions=True, do_print=True):
    if (os.path.isfile(aux_file)):
        os.remove(aux_file)
    with open(aux_file, "w") as fh:
        fh.write("Cannot access file: " + aux_file)
    os.chmod(aux_file, 0o777)

    success_msg = 'Success transfering authorization key for: ' + user
    cmd =  'echo Authorizing: ' + user + ";"
    cmd = cmd + 'echo Invalid user or password > ' + aux_file + ";"
    cmd = cmd + "export PK_SUCCESS=FAILURE;"
    if RSYNC_GENERATE_USER_KEY:
        reset_user_key(do_print)
        cmd = cmd + "export PK=`cat ~/.ssh/ke.pub`;"
    else:
        cmd = cmd + "export PK=`cat ~/.ssh/id_rsa.pub`;"
    cmd = cmd + 'su - ' + user + ' bash -c "'
    cmd = cmd + 'echo $PK >> .ssh/authorized_keys;'
    #cmd = cmd + 'sort .ssh/authorized_keys | uniq > .ssh/authorized_keys.uniq;'
    #cmd = cmd + 'mv .ssh/authorized_keys.uniq .ssh/authorized_keys;'
    if fix_permissions:
        cmd = cmd + 'chmod g-w ~' + ";"
    cmd = cmd + 'echo ' + success_msg + ";"
    cmd = cmd + 'echo ' + success_msg + " > " + aux_file + ";"
    cmd = cmd + '"'
    #xterm_options = '-hold -T "Authentication" -into 44040199' #Get Winfow ID with 'wmctrl -lp'
    xterm_options = '-T "Authentication" -fa monaco -fs 14 -bg black -fg green -geometry 80x15+400+100'
    try:
        ret = exec_cmd("xterm " + xterm_options + " -e '" + cmd + "'")
        with open (aux_file, "r") as myfile:
            ret=myfile.read()
        #;if [ "$depth" -eq "1" ]; then echo ' + success_msg + '; fi')
        if not success_msg in ret:
            raise Exception (ret)
    except:
        if RSYNC_GENERATE_USER_KEY:
            remove_user_key(do_print)
        raise  Exception ("Error authenticating user: " + str(sys.exc_info()[1]))
    finally:
        if (os.path.isfile(aux_file)):
            os.remove(aux_file)
    return ret

