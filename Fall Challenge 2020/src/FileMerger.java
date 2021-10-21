import java.io.*;

public class FileMerger {
    public static void main(String[] args) throws Exception {
        while(true) {
            File sourceDir = new File("src");
            BufferedReader reader;
            String line;
            StringBuilder classContents = new StringBuilder();

            File mergedCode = new File("Full Code.java");
            mergedCode.createNewFile();
            PrintWriter writer = new PrintWriter(new FileOutputStream(mergedCode));

            for (File javaFile : sourceDir.listFiles()) {

                if (javaFile.getPath().equals("src\\FileMerger.java"))  //ignore this file
                    continue;

                reader = new BufferedReader(new FileReader(javaFile));
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("import "))  //write import statements
                        writer.println(line);
                    else
                        classContents.append(line).append("\n");
                }
                reader.close();
            }
            //write class contents
            writer.println(classContents.toString());
            writer.close();

            System.out.println("Files merged. Code: " + (int) (Math.random() * 100));
            Thread.sleep(3000);
        }
    }
}
