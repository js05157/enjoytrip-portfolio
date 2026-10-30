package com.enjoytrip.place;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ImageStoreTest {
  @TempDir Path dir;

  @Test
  void rejectsNonImagesAndPathsAndReencodesSafeImage() throws Exception {
    String previous = System.getProperty("catalina.base");
    System.setProperty("catalina.base", dir.toString());
    try {
      ImageStore store = new ImageStore();
      assertThrows(
          IllegalArgumentException.class,
          () -> store.save(new ByteArrayInputStream("<svg>bad</svg>".getBytes())));
      assertThrows(IllegalArgumentException.class, () -> store.file("../../secret"));
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
      String name = store.save(new ByteArrayInputStream(bytes.toByteArray()));
      assertTrue(Files.exists(store.file(name)));
      assertNotNull(ImageIO.read(store.file(name).toFile()));
      store.delete(name);
      assertFalse(Files.exists(store.file(name)));
    } finally {
      if (previous == null) System.clearProperty("catalina.base");
      else System.setProperty("catalina.base", previous);
    }
  }
}
