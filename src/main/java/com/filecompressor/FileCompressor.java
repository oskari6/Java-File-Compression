package com.filecompressor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javafx.css.Match;

public class FileCompressor {
    private static final int SEARCH_BUFFER_SIZE = 1024;
    private static final int LOOKAHEAD_BUFFER_SIZE = 16;
    /*LZ77 file compression*/
   
    public File compress(File file) throws IOException {
        File compressedFile = new File(file.getParent(), file.getName() +".lz77");
        File readableFile = new File(file.getParent(), file.getName() + ".debug.txt"); // Human-readable file

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
            DataOutputStream output = new DataOutputStream(new FileOutputStream(compressedFile));
         BufferedWriter debugWriter = new BufferedWriter(new FileWriter(readableFile))) {

            StringBuilder searchBuffer = new StringBuilder();
            char[] lookaheadBuffer = new char[LOOKAHEAD_BUFFER_SIZE];
            int bytesRead;

            while((bytesRead = reader.read(lookaheadBuffer)) != -1){
                int i = 0;
                while (i < bytesRead) {
                    String lookaheadSubstring = new String(lookaheadBuffer, i, bytesRead - i);
                    Match match = findLongestMatch(searchBuffer.toString(), lookaheadSubstring);

                    if(match.length > 1){
                        output.writeByte(1);
                        output.writeShort(match.offset);
                        output.writeShort(match.length);
                        debugWriter.write(String.format("Match: Offset=%d, Length=%d%n", match.offset, match.length));

                        i += match.length;
                    }else{
                        output.writeByte(0);
                        output.writeByte((byte)lookaheadBuffer[i]);
                        debugWriter.write(String.format("Char: %c%n", lookaheadBuffer[i]));
                        i++;
                    }

                    searchBuffer.append(lookaheadBuffer[i - 1]);
                    if(searchBuffer.length() > SEARCH_BUFFER_SIZE){
                        searchBuffer.delete(0, searchBuffer.length() - SEARCH_BUFFER_SIZE);
                    }
                }
            }
        }
        return compressedFile;
    }

    public File decompress(File file) throws IOException {
        if(!file.getName().endsWith(".lz77")){
            throw new IllegalArgumentException("File is not in correct format(.lz77)");
        }
        
        File decompressedFile = new File(file.getParent(), file.getName().replace(".lz77", "decompressed"));
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
