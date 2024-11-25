package com.filecompressor;

import java.io.*;

public class SmallFileCompressor {

    // Compress the file using Run-Length Encoding
    public File compress(File file) throws IOException {
        File compressedFile = new File(file.getParent(), file.getName() + ".rle");

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             DataOutputStream output = new DataOutputStream(new FileOutputStream(compressedFile))) {

            int prevChar = -1;
            int count = 0;

            int currChar;
            while ((currChar = reader.read()) != -1) {
                if (currChar == prevChar) {
                    count++;
                    if (count == 255) { // Max byte value for RLE
                        output.writeByte(count);
                        output.writeByte(currChar);
                        count = 0;
                    }
                } else {
                    if (prevChar != -1) {
                        output.writeByte(count);
                        output.writeByte(prevChar);
                    }
                    prevChar = currChar;
                    count = 1;
                }
            }

            // Write the last run
            if (prevChar != -1) {
                output.writeByte(count);
                output.writeByte(prevChar);
            }
        }

        return compressedFile;
    }

    // Decompress the file using Run-Length Encoding
    public File decompress(File file) throws IOException {
        if (!file.exists() || !file.isFile() || !file.getName().endsWith(".rle")) {
            throw new IllegalArgumentException("Unsupported file format. File must have a '.rle' extension.");
        }

        File decompressedFile = new File(file.getParent(), "decompressed-" + file.getName().replace(".rle", ""));

        try (DataInputStream input = new DataInputStream(new FileInputStream(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(decompressedFile))) {

            while (input.available() > 0) {
                int count = input.readUnsignedByte();
                int character = input.readUnsignedByte();

                for (int i = 0; i < count; i++) {
                    writer.write(character);
                }
            }
        }

        return decompressedFile;
    }
}
