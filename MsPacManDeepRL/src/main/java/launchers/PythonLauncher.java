package launchers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class PythonLauncher {
    public static void launchScript(String...commands) throws Exception {
    	// PROCESS BUILDER API JAVA
    	String file = "";
    	if (commands[0].contains("edible"))
    		file = "src/main/java/launchers/LoadMsPacManEdible.py";
    	else if (commands[0].contains("notEdible"))
    		file = "src/main/java/launchers/LoadMsPacManNotEdible.py";
    	else
    		throw new Exception("Invalid argument 0");
    	ProcessBuilder processBuilder = new ProcessBuilder("python3", file, commands[0], commands[1]);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        List<String> results = readProcessOutput(process.getInputStream());

        for (String s : results)
        	System.out.println(s);
	}
    
    private static List<String> readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader output = new BufferedReader(new InputStreamReader(inputStream))) {
            return output.lines().collect(Collectors.toList());
        }
    }
	
	public static void main(String[] args) {
		try {
			launchScript();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
