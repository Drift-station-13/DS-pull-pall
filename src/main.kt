import dspal2.run_OS_command
import dspal2.task_start
import java.io.File

fun log(msg: String){
    println(msg);
}

fun main(args: Array<String>) {
    // Setup local deps
    listOf<String>("ds-patch-pal.exe").forEach(){
        if(!File(it).exists()){
            log(it + " does not exist.");
            return;
        }
    }

    val deps = listOf<String>("filterdiff.exe", "wiggle.exe", "gp.exe", "cygwin1.dll", "cygncursesw-10.dll")
    var i = 0;
    while(deps.count() > i)
    {
        if(!File(deps[i]).exists()){
            log("Extracting deps.");
            run_OS_command("ds-patch-pal.exe");
            break;
        }
        i++;
    }

    // ARGS
    if(args.isEmpty()){
        log("Please provide a github pull request link as the first argument.");
        return;
    }

    //task_start("https://github.com/Citadel-Station-13/Citadel-Station-13/pull/9086/files", "../..");
    task_start(args[0],  "../..");

}