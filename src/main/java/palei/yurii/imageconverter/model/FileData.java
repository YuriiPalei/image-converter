package palei.yurii.imageconverter.model;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;

@Getter
@ToString
public class FileData {
  private final VirtualFile file;
  private final File ioFile;
  private final long fileSize;
  @Setter private boolean selected;
  @Setter private Long estimatedSize;
  @Setter private Double reductionPercentage;
  @Setter private String status;

  public FileData(
      VirtualFile file,
      boolean selected,
      long fileSize) {
    this.file = file;
    this.selected = selected;
    this.fileSize = fileSize;
    this.ioFile = new File(file.getPath());
    this.status = "Estimating...";
  }
}
