package com.enjoytrip.place;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

public final class ImageStore {
  private final Path root;

  public ImageStore() {
    root =
        Path.of(System.getProperty("catalina.base", "."), "enjoytrip-data", "uploads")
            .toAbsolutePath();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public String save(InputStream source) throws IOException {
    try (ImageInputStream input = ImageIO.createImageInputStream(source)) {
      if (input == null) throw new IllegalArgumentException("사진 파일을 확인하세요.");
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw new IllegalArgumentException("JPEG 또는 PNG 사진을 선택하세요.");
      ImageReader reader = readers.next();
      try {
        reader.setInput(input);
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!Set.of("jpeg", "jpg", "png").contains(format))
          throw new IllegalArgumentException("JPEG 또는 PNG 사진만 지원합니다.");
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 1
            || height < 1
            || width > 6000
            || height > 6000
            || (long) width * height > 12_000_000)
          throw new IllegalArgumentException("사진은 1200만 화소 이하로 업로드하세요.");
        BufferedImage image = reader.read(0);
        double ratio = Math.min(1, 2048.0 / Math.max(width, height));
        BufferedImage safe =
            new BufferedImage(
                Math.max(1, (int) (width * ratio)),
                Math.max(1, (int) (height * ratio)),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = safe.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, safe.getWidth(), safe.getHeight());
        g.drawImage(image, 0, 0, safe.getWidth(), safe.getHeight(), null);
        g.dispose();
        String name = UUID.randomUUID() + ".jpg";
        try (OutputStream output =
            Files.newOutputStream(root.resolve(name), StandardOpenOption.CREATE_NEW)) {
          if (!ImageIO.write(safe, "jpg", output)) throw new IOException("사진 저장 실패");
        }
        return name;
      } finally {
        reader.dispose();
      }
    }
  }

  public Path file(String name) {
    if (!name.matches("[a-f0-9-]{36}\\.jpg")) throw new IllegalArgumentException("사진 이름이 잘못되었습니다.");
    return root.resolve(name);
  }

  public void delete(String name) {
    try {
      Files.deleteIfExists(file(name));
    } catch (IOException ignored) {
    }
  }
}
