package com.example.petlife.service;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Method;

final class ImageOrientationUtil {

    private ImageOrientationUtil() {
    }

    static int readOrientation(InputStream in) {
        try {
            Class<?> readerClass = Class.forName("com.drew.imaging.ImageMetadataReader");
            Method readMetadata = readerClass.getMethod("readMetadata", InputStream.class);
            Object metadata = readMetadata.invoke(null, in);

            Class<?> exifClass = Class.forName("com.drew.metadata.exif.ExifIFD0Directory");
            Method getFirstDirectoryOfType = metadata.getClass().getMethod("getFirstDirectoryOfType", Class.class);
            Object exif = getFirstDirectoryOfType.invoke(metadata, exifClass);
            if (exif == null) return 1;

            int orientationTag = exifClass.getField("TAG_ORIENTATION").getInt(null);
            Method getInt = exifClass.getMethod("getInt", int.class);
            return (int) getInt.invoke(exif, orientationTag);
        } catch (Exception ignored) {
            return 1;
        }
    }

    static BufferedImage applyOrientation(BufferedImage src, int orientation) {
        return switch (orientation) {
            case 3 -> rotate180(src);
            case 6 -> rotate90CW(src);
            case 8 -> rotate90CCW(src);
            default -> src;
        };
    }

    private static BufferedImage rotate90CW(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getHeight(), src.getWidth(), imageType(src));
        AffineTransform tx = new AffineTransform();
        tx.translate(src.getHeight(), 0);
        tx.rotate(Math.toRadians(90));
        apply(src, dst, tx);
        return dst;
    }

    private static BufferedImage rotate90CCW(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getHeight(), src.getWidth(), imageType(src));
        AffineTransform tx = new AffineTransform();
        tx.translate(0, src.getWidth());
        tx.rotate(Math.toRadians(-90));
        apply(src, dst, tx);
        return dst;
    }

    private static BufferedImage rotate180(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), imageType(src));
        AffineTransform tx = new AffineTransform();
        tx.translate(src.getWidth(), src.getHeight());
        tx.rotate(Math.toRadians(180));
        apply(src, dst, tx);
        return dst;
    }

    private static void apply(BufferedImage src, BufferedImage dst, AffineTransform tx) {
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        op.filter(src, dst);
    }

    private static int imageType(BufferedImage src) {
        return src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    }
}
