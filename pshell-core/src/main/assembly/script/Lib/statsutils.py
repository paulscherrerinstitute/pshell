###################################################################################################
# Utilities for generating reports from command statistics files
###################################################################################################

#CsvJdbc JAR file must be downloaded to extensions folder:
#http://central.maven.org/maven2/net/sourceforge/csvjdbc/csvjdbc/1.0.34/csvjdbc-1.0.34.jar


import java.sql.DriverManager as DriverManager
import java.sql.ResultSet as ResultSet
import java.util.Properties as Properties
import java.lang.Class as Class
import os
from startup import get_interpreter, expand_path
import ch.psi.pshell.sequencer.CommandStatistics as CommandStatistics
import ch.psi.pshell.sequencer.CommandStatistics.FileRange as CommandStatisticsFileRange

stmt = None 
STAT_COLUMN_NAMES = ["Command","Args","Source","Start","End","Background","Result","Return"]
def get_stats_connection():
    global stmt
    Class.forName("org.relique.jdbc.csv.CsvDriver");   
    db = os.path.abspath(expand_path("{home}/statistics"))
    props = Properties()
    props.put("fileExtension", ".csv")
    props.put("separator", ";")     
    props.put("timestampFormat", "dd/MM/yy HH:mm:ss.SSS")      
    props.put("indexedFiles", "true");
    props.put("columnTypes", "String,String,String,Timestamp,Timestamp,Boolean,String,String");
    
    fileRange = CommandStatistics.getConfig().fileRange    
    if fileRange==CommandStatisticsFileRange.Daily:
        props.put("fileTailPattern", "(\\d+)_(\\d+)_(\\d+)"); 
        props.put("fileTailParts", "Year,Month,Day");
    elif fileRange==CommandStatisticsFileRange.Monthly:
        props.put("fileTailPattern", "(\\d+)_(\\d+)"); #props.put("fileTailPattern", "-(\\d+)_(\\d+)");
        props.put("fileTailParts", "Year,Month");
    elif fileRange==CommandStatisticsFileRange.Yearly:
        props.put("fileTailPattern", "(\\d+)"); 
        props.put("fileTailParts", "Year");    
 
    conn = DriverManager.getConnection("jdbc:relique:csv:" + db, props);
    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
    return conn

def _get_count(sql):
    ret = 0
    results = stmt.executeQuery("SELECT COUNT(*) AS count FROM . WHERE " + sql) 
    if results.first():
        ret = results.getInt("count")    
    return ret

def _add_sql_time(sql, start, end):
    if start:
        if len(start)==8:
            start = start + " 00:00:00.000"          
        sql = sql + " AND Start>='" + start + "'"
    if end:
        if len(end)==8:
            end = end + " 00:00:00.000"          
        sql = sql +  " AND (\"End\"<'" + end + "')"    
    return sql   
        
def get_count(command= "%%", start = None, end = None, result= "%%"):
    sql = "Command LIKE '"+ command +"' AND Result LIKE '"+ result +"'"
    sql = _add_sql_time(sql, start, end)
    return _get_count(sql)

def get_return_count(command= "%%", start = None, end = None, ret= "%%"):
    sql = "Command LIKE '"+ command +"' AND Return = '"+ ret +"'"
    sql = _add_sql_time(sql, start, end)
    return _get_count(sql)    

def get_cmd_stats(command = "%", start = None, end = None):
    s = get_count(command, start, end, "success")
    a = get_count(command, start, end, "abort")
    e = get_count(command, start, end, "error")
    return (s,a,e)

def get_errors(command = "%", start = None, end = None):
    sql = "SELECT Return, Count(Return) as count FROM . WHERE Command LIKE '"+ command +"' AND Result='error'"
    sql = _add_sql_time(sql, start, end)
    sql = sql + " GROUP BY Return ORDER BY count DESC"
    results = stmt.executeQuery(sql) 
    ret = []
    while results.next():  
        ret.append((results.getInt("count"), results.getString("Return")))           
    return ret


def get_cmd_records(command = "%", start = None, end = None, result= "%%"):
    sql = "SELECT * FROM . WHERE Command LIKE '"+ command +"' AND Result LIKE '"+ result +"'"
    sql = _add_sql_time(sql, start, end)
    results = stmt.executeQuery(sql) 
    ret = []
    while results.next():  
        rec={}
        for col in STAT_COLUMN_NAMES:
            rec[col]= results.getString(col)
        ret.append(rec)           
    return ret    

def get_commands(commands =None, start = None, end = None):
    ret = []
    if (commands is None) or (len(commands)==0):
        sql = "SELECT * FROM . WHERE Command != ''"
        sql = _add_sql_time(sql, start, end)
        sql = sql + " GROUP BY Command"
        results = stmt.executeQuery(sql) 
        while results.next():  
            cmd = results.getString("Command")
            if cmd and not " " in cmd:  
                ret.append(cmd)           
    else:
        for cmd in commands:
            if get_count(cmd, start, end) >0 :
                ret.append(cmd)  
    return ret

def print_cmd_stats(command = "%", start = None, end = None):
    print "-----------------------------------------------------------"
    print "Statistics from ",  start  , " to ",  end 
    (s,a,e) = get_cmd_stats(command, start, end)
    t=s+a+e #get_count(command, start, end, "%")
    print "Command: " , command , " Records: ", t    
    if t>0: 
        print "%-10s %7.2f%% - %d" % ("Success", (float(s)/t) * 100, s)
        print "%-10s %7.2f%% - %d" % ("Abort",   (float(a)/t) * 100, a)
        print "%-10s %7.2f%% - %d" % ("Error",   (float(e)/t) * 100, e)
        
        print "\nErrors:"
        print "%5s   %s" % ("Count", "Error")
        errors =  get_errors(command, start, end)    
        for error in errors:
            print "%5d   %s" % (error[0], error[1])
    print "-----------------------------------------------------------"     

def print_cmd_records(command = "%", start = None, end = None, result= "%%"):
    print "-----------------------------------------------------------"
    print "Records from ",  start  , " to ",  end 
    info = get_cmd_records(command, start, end, result)
    print "Command: " , command , " Result: ", result,  " Records: ", len(info)  
        
    for col in STAT_COLUMN_NAMES:
        print col+ "; " ,
    print
    
    for cmd in info:
        s = "" 
        for col in STAT_COLUMN_NAMES:
            s = s + cmd[col]+ "; "
        print s
    print "-----------------------------------------------------------"  
       
def print_stats(commands = None, start = None, end = None):
    print "-----------------------------------------------------------"
    print "Statistics from ",  start  , " to ",  end 
    print "%-20s %-5s %8s %8s %8s" % ("Command", "Total", "Success", "Abort", "Error")
    cmds = get_commands(commands)   
    for cmd in cmds:
        (s,a,e) = get_cmd_stats(cmd, start, end)
        t=s+a+e 
        if t>0:
            print "%-20s %-5d %7.2f%% %7.2f%% %7.2f%%" % (cmd, t, (float(s)/t) * 100, (float(a)/t) * 100, (float(e)/t) * 100)
        else:
            print "%-20s %-5d" % (cmd, t)
    print "-----------------------------------------------------------"  
        



if __name__=='__main__': 
    conn = get_stats_connection()
    
    #Print stats of all commands, with no time range
    print_stats()

    cmds = ["%scan1%", "%scan2%"]
    start= "01/03/19" 
    end=   "01/04/19"  
       
    #Print stats all commands containing 'scan1' and 'scan2' in the month 03.2019
    print_stats(cmds, start, end)
    
    #Print individual statistics, including error count, for commands containing 'scan1' and 'scan2'
    for cmd in cmds:
        print_cmd_stats (cmd, start, end)
    
    #Print all records for commands containing 'scan1'
    print_cmd_records("%scan1%%", start, end, "error")        
    conn.close()   

