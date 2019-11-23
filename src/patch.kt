package dspal2

import java.io.File
import java.net.URL

val DME_FILE_NAME = "yori_station.dme";

fun log(msg: String){
    println(msg);
}
fun log_title(msg: String){
    log(" -=-=-=-= " + msg + " =-=-=-=- ");
}
fun error(err: String){
    log("[ds-pal] error: " + err);
}

@Throws(java.io.IOException::class)
fun run_OS_command(cmd: String): String {
    File("run.bat").writeText(cmd);
    val exec = Runtime.getRuntime().exec("run.bat");
    val s = java.util.Scanner(exec.inputStream).useDelimiter("\\A")
    exec.waitFor()
    File("run.bat").delete();
    return if (s.hasNext()) s.next() else ""
}



fun is_rep_path_ok(rep_path: String): Boolean{
    val file_p = File(rep_path);
    if(file_p.exists()){
        if(file_p.isDirectory()){
            if(File(rep_path + '/' + DME_FILE_NAME).exists() && File(rep_path + '/' + DME_FILE_NAME).isFile){
                return true;
            }else{
                error("This is not the Drift Station repo!");
            }
        }else{
            error("Path is not a directory.");
            error("given path: " + file_p + ", canonical path: " + file_p.canonicalFile);
        }
    }else{
        error("Path is not valid.");
        error("given path: " + file_p);
    }
    return false;
}

fun task_start(url: String, path: String){
    val rep_path = File(path).canonicalPath;
    if(!is_rep_path_ok(rep_path)){
        error("given path is not a valid Drift Station repo.");
        return;
    }

    log_title("starting");
    // PARSING & PROCESSING URL
    var url_split = url.split('/');
    //      CHECK IF URL IS VALID
    if(url_split.count() < 6){
        error("url is not a github pull request. (less then 6 pram)");
        return;
    }
    if(!(url_split[2] == "github.com" && url_split[5] == "pull")) {
        error("url is not a github pull request. (template not matched)");
        return;
    }

    // DOWNLOAD PATCH FILE.
    val patch_file_name = url_split[6];
    val patch_file = File("./" + patch_file_name + ".patch");
    // check if its already downloaded.
    if(!patch_file.exists()){
        // download the file
        patch_file.writeText(URL("https://patch-diff.githubusercontent.com/raw/" + url_split[3] + '/' + url_split[4] + "/pull/" + url_split[6] + ".patch").readText());
        log("downloaded: " + patch_file_name);
    }

    task_simple_patch(patch_file.canonicalPath, rep_path);
}


fun task_simple_patch(patch: String, rep_path: String){
    log_title("patching");
    log( run_OS_command("filterdiff.exe --exclude='**.dme' --exclude='**.dmi' --exclude='**.dmm' --include='**.dm' "+patch+" --clean > code.patch"));
    log_title("running gp");
    var got = run_OS_command("gp.exe --fuzz 500 --forward --input="+File("code.patch").canonicalFile+" --remove-empty-files -f --binary -p1 --directory="+rep_path);
    log(got);
    log_title("trying to fix rejects")

    var rej_files: MutableList<String> = mutableListOf();
    got.lines().forEach {
        if(it.contains(" -- saving rejects to file ")){
            rej_files.add(File(it.split(" -- saving rejects to file ")[1]).canonicalPath);
        }
    }
    rej_files.forEach {
        log(it);
        var got = run_OS_command("wiggle.exe --replace "+it.removeSuffix(".rej")+' '+it);
        if(got.contains("unresolved conflict found")){
            log("   requires manual editing.");
        }
    }

    // DME
    run_OS_command("filterdiff.exe --include='*.dme' "+patch+" --clean > dme.patch");
    run_OS_command("gp.exe --binary -f Drift_station_13/yori_station.dme "+File("dme.patch").canonicalFile);
}