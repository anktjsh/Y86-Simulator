
package virtual.machine.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author aniket
 */
public class Script {

    private final File file;
    private String lastCode;

    public Script(File f, String code) {
        file = f;
        lastCode = code;
        initScript(code);
    }

    public void reload() {
        lastCode = readCode();
    }

    public boolean canSave(String code) {
        return !code.equals(lastCode);
    }

    public void save(String code) {
        try {
            Files.write(file.toPath(), code.getBytes());
        } catch (IOException ex) {
        }
        lastCode = code;
    }

    public String getCurrentCode() {
        return lastCode;
    }

    private void initScript(String list) {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                Files.write(getFile().toPath(), list.getBytes());
            } catch (IOException ex) {
            }
        } else {
            lastCode = readCode();
        }

    }

    private String readCode() {
        StringBuilder sb = new StringBuilder();
        try {
            List<String> read = Files.readAllLines(file.toPath());
            read.forEach((s) -> {
                sb.append(s).append("\n");
            });
        } catch (IOException ex) {
        }
        return sb.toString();
    }

    public File getFile() {
        return file;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Script) {
            Script pro = (Script) obj;
            if (pro.getFile().equals(getFile())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.file);
        hash = 97 * hash + Objects.hashCode(this.lastCode);
        return hash;
    }

}
