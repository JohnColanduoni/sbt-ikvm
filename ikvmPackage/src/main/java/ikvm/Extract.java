package ikvm;

import java.io.*;

public class Extract {
    public static void extract(File target) throws IOException {
        try(InputStream stream = Extract.class.getResourceAsStream("files_list")) {
            if (stream == null)
                throw new IllegalStateException("Missing files list resource.");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            byte[] buffer = new byte[4096];

            for (String name; (name = reader.readLine()) != null; ) {
                try (InputStream inputStream = Extract.class.getResourceAsStream(name)) {
                    if (inputStream == null)
                        throw new IllegalStateException("Missing '" + name + "'.");

                    File outputFile = new File(target, name);
                    File parentFile = outputFile.getParentFile();
                    if(!parentFile.exists() && !parentFile.mkdirs())
                        throw new IOException("Failed to create necessary directories.");

                    try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                        for (int bytes; (bytes = inputStream.read(buffer)) != -1; ) {
                            outputStream.write(buffer, 0, bytes);
                        }
                    }
                }
            }
        }
    }
}
