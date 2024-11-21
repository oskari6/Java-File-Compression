package com.filecompressor;

import java.io.*;
import java.util.*;

public class SmallFileCompressor {
    /*Huffman coding file compression */
    static class Node implements Comparable<Node>{
        char character;
        int frequency;
        Node left, right;

        Node(char character, int frequency){
            this.character = character;
            this.frequency = frequency;
        }

        Node(int frequency, Node left, Node right){
            this.frequency = frequency;
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(Node other){
            return this.frequency - other.frequency;
        }
    }

    public File compress(File file) throws IOException{
        System.out.println("in");
        //calculate frequencies of characters
        Map<Character, Integer> frequenceyMap = calculateFrequencies(file);
        //create tree
        Node root = buildHuffmanTree(frequenceyMap);
        //generate codes
        Map<Character, String>huffmanCodes = generateHuffmanCodes(root);
        //encode file
        File compressedFile = new File(file.getParent(), file.getName() + ".huffman");
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             DataOutputStream output = new DataOutputStream(new FileOutputStream(compressedFile))) {
                //write to compressed file
                writeHuffmanTree(output, root);
                //write endcoded content
                int character;
                StringBuilder encodedContent = new StringBuilder();
                while((character = reader.read()) != -1){
                    encodedContent.append(huffmanCodes.get((char) character));
                }
                //write as bits
                writeBits(output, encodedContent.toString());
            }
            return compressedFile;
    }

    private Map<Character, Integer> calculateFrequencies(File file) throws IOException {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
            int character;
            while((character = reader.read()) != -1){
                frequencyMap.put((char) character, frequencyMap.getOrDefault((char) character ,0) +1);
            }
        }
        return frequencyMap;
    }

    private Node buildHuffmanTree(Map<Character, Integer> frequencyMap){
        PriorityQueue<Node> queue = new PriorityQueue<>();
        for(Map.Entry<Character, Integer> entry : frequencyMap.entrySet()){
            queue.add(new Node(entry.getKey(), entry.getValue()));
        }

        while(queue.size() > 1){
            Node left = queue.poll();
            Node right = queue.poll();
            queue.add(new Node(left.frequency + right.frequency, left, right));
        }
        return queue.poll();
    }

    private Map<Character, String> generateHuffmanCodes(Node root){
        Map<Character, String> huffmanCodes = new HashMap<>();
        generateCodesRecursive(root, "", huffmanCodes);
        return huffmanCodes;
    }

    private void generateCodesRecursive(Node node, String code, Map<Character, String> huffmanCodes){
        if(node == null) return;
        if(node.left == null || node.right == null){
            huffmanCodes.put(node.character, code);
        }
        //most frequent characters get shorter code ie 0, 10
        generateCodesRecursive(node.left, code + "0", huffmanCodes);
        generateCodesRecursive(node.right, code + "1", huffmanCodes);
    }

    private void writeHuffmanTree(DataOutputStream output, Node node) throws IOException {
        if(node.left == null && node.right == null){
            output.writeBoolean(true);
            output.writeChar(node.character);
        }else{
            output.writeBoolean(false);
            writeHuffmanTree(output, node.left);
            writeHuffmanTree(output, node.right);
        }
    }

    private void writeBits(DataOutputStream output, String encodedContent) throws IOException {
        int buffer = 0, bitCount = 0;

        for(char bit : encodedContent.toCharArray()){
            //<< left shift everything to left by 1
            //bit - "0" converts "0" to integer 0
            // | new bit to least significant position in buffer
            buffer = (buffer << 1) | (bit - '0');
            bitCount++;

            if(bitCount == 8){
                output.writeByte(buffer);
                buffer = 0;
                bitCount = 0;
            }
        }
        if(bitCount > 0){
            buffer <<= (8 - bitCount); //Ensures that any leftover bits in the buffer are written correctly as a full byte.
            output.writeByte(buffer);
        }
    }

    public File decompress (File file) throws IOException{
        if(!file.getName().endsWith(".huffman")){
            throw new IllegalArgumentException("File is not in Huffman format (.huffman)");
        }

        File decompressedFile = new File(file.getParent(), "decompressed-" + file.getName().replace(".huffman", ""));
        try (DataInputStream input = new DataInputStream(new FileInputStream(file));
            BufferedWriter writer = new BufferedWriter(new FileWriter(decompressedFile))){
                Node root = readHuffmanTree(input);

                Node current = root;
                int bit;
                while(input.available() > 0){
                    int b = input.readUnsignedByte();
                    for(int i = 7; i >= 0; i--){
                        bit = (b >> i) & 1;
                        current = (bit == 0) ? current.right : current.right;

                        if(current.left == null && current.right == null){
                            writer.write(current.character);
                            current = root;
                        }
                    }
                }
            }
            return decompressedFile;
    }
    private Node readHuffmanTree(DataInputStream input) throws IOException {
        if(input.readBoolean()){
            return new Node(input.readChar(), 0);
        }else{
            return new Node(0, readHuffmanTree(input), readHuffmanTree(input));
        }
    }
}
