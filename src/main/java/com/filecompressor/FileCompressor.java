package com.filecompressor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileCompressor {
    private static int SEARCH_BUFFER_SIZE = 1024;
    private static int LOOKAHEAD_BUFFER_SIZE = 16;
    /*LZ77 file compression*/
   
    public File compress(File file) throws IOException {
        File compressedFile = new File(file.getParent(), file.getName() + ".lz77");
        
        long fileSize = file.length();
        //huffman coding
        if(fileSize  <= 100){
            SmallFileCompressor smallFileCompressor = new SmallFileCompressor();
            return smallFileCompressor.compress(file);
        } if (fileSize <= 1024) { // File size <= 1 KB
            SEARCH_BUFFER_SIZE = 512;
            LOOKAHEAD_BUFFER_SIZE = 16;
        } else if (fileSize <= 10240) { // File size <= 10 KB
            SEARCH_BUFFER_SIZE = 1024;
            LOOKAHEAD_BUFFER_SIZE = 64;
        } else if (fileSize <= 1048576) { // File size <= 1 MB
            SEARCH_BUFFER_SIZE = 8192;
            LOOKAHEAD_BUFFER_SIZE = 128;
        } else { // File size > 1 MB
            SEARCH_BUFFER_SIZE = 32768;
            LOOKAHEAD_BUFFER_SIZE = 256;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             DataOutputStream output = new DataOutputStream(new FileOutputStream(compressedFile))) {
    
            char[] searchBuffer = new char[SEARCH_BUFFER_SIZE];
            int searchBufferEnd = 0; // Tracks the end of valid data
            char[] lookaheadBuffer = new char[LOOKAHEAD_BUFFER_SIZE];
            int bytesRead;
    
            while ((bytesRead = reader.read(lookaheadBuffer)) != -1) {
                int i = 0;
    
                while (i < bytesRead) {
                    String searchBufferContent = new String(searchBuffer, 0, searchBufferEnd);
                    String lookaheadSubstring = new String(lookaheadBuffer, i, bytesRead - i);
                    Match match = findLongestMatch(searchBufferContent, lookaheadSubstring);
    
                    if (match.length > 1) { // Write match
                        output.writeByte(1); // Match flag
                        output.writeShort(match.offset);
                        output.writeShort(match.length);
    
                        for (int j = 0; j < match.length; j++) {
                            // Add matched characters to circular buffer
                            searchBuffer[searchBufferEnd % SEARCH_BUFFER_SIZE] = lookaheadBuffer[i + j];
                            searchBufferEnd = (searchBufferEnd + 1) % SEARCH_BUFFER_SIZE;
                        }
                        i += match.length;
                    } else { // Write single character
                        output.writeByte(0); // Single character flag
                        output.writeByte((byte) lookaheadBuffer[i]);
                        searchBuffer[searchBufferEnd % SEARCH_BUFFER_SIZE] = lookaheadBuffer[i];
                        searchBufferEnd = (searchBufferEnd + 1) % SEARCH_BUFFER_SIZE;
                        i++;
                    }
                }
            }
        }
    
        return compressedFile;
    }

    public File decompress(File file) throws IOException {
        if (!(file.getName().endsWith(".lz77") || file.getName().endsWith(".bpe"))) {
            throw new IllegalArgumentException("Unsupported file format: " + file.getName());
        }
        if (file.getName().endsWith(".bpe")) {
            SmallFileCompressor smallFileCompressor = new SmallFileCompressor();
            return smallFileCompressor.decompress(file);
        }
        File decompressedFile = new File(file.getParent(), "decompressed-" + file.getName().replace(".lz77", ""));
        try (DataInputStream input = new DataInputStream(new FileInputStream(file));
            BufferedWriter writer = new BufferedWriter(new FileWriter(decompressedFile))){
            
            StringBuilder decompressedData = new StringBuilder();

            while(input.available() > 0){
                byte flag = input.readByte();
                if(flag == 1){
                    int offset = input.readShort();
                    int length = input.readShort();
                    int startIndex = decompressedData.length() - offset;
                    for(int i = 0; i < length; i++){
                        decompressedData.append(decompressedData.charAt(startIndex +i));
                    }
                }else{
                    byte nextChar = input.readByte();
                    decompressedData.append((char)nextChar);
                }
            }
            writer.write(decompressedData.toString());
        }
        return decompressedFile;
    }

    private Match findLongestMatch(String searchBuffer, String lookaheadBuffer){
        int maxLength = 0;
        int bestOffset = 0;

        for(int i = 0; i < searchBuffer.length(); i++){
            int length = 0;
            while (length < lookaheadBuffer.length() &&
             (i + length) < searchBuffer.length() &&
             searchBuffer.charAt(i + length) == lookaheadBuffer.charAt(length)){
                length++;
            }
            if(length > maxLength){
                maxLength = length;
                bestOffset = searchBuffer.length() - i;
            }
        }
        return new Match(bestOffset, maxLength);
    }

    private static class Match{
        int offset;
        int length;

        public Match(int offset, int length){
            this.offset = offset;
            this.length = length;
        }
    }
}
