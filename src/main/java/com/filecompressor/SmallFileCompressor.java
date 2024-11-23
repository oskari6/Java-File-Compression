package com.filecompressor;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class SmallFileCompressor {
    /* Huffman coding file compression */
    static class Node implements Comparable<Node> {
        char character;
        int frequency;
        Node left, right;

        Node(char character, int frequency) {
            this.character = character;
            this.frequency = frequency;
        }

        Node(int frequency, Node left, Node right) {
            this.frequency = frequency;
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(Node other) {
            return this.frequency - other.frequency;
        }
    }

    public File compress(File file) throws IOException {
        Map<Character, Integer> frequencyMap = calculateFrequencies(file); // Calculate frequencies of characters
        if (frequencyMap.size() == 1) { // Special case: single character
            File compressedFile = new File(file.getParent(), file.getName() + ".huffman");
            try (DataOutputStream output = new DataOutputStream(new FileOutputStream(compressedFile))) {
                output.writeBoolean(true); // Special case flag
                Map.Entry<Character, Integer> entry = frequencyMap.entrySet().iterator().next();
                output.writeChar(entry.getKey()); // Single character
                output.writeShort(entry.getValue()); // Frequency
            }
            return compressedFile;
        }

        Node root = buildHuffmanTree(frequencyMap); // Create tree
        Map<Character, String> huffmanCodes = generateHuffmanCodes(root); // Generate codes
        File compressedFile = new File(file.getParent(), file.getName() + ".huffman"); // Encoded file

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedReader reader = new BufferedReader(new FileReader(file));
             DataOutputStream output = new DataOutputStream(byteArrayOutputStream)) {

            StringBuilder treeBitStream = new StringBuilder();
            writeHuffmanTree(output, root, treeBitStream); // Write Huffman tree
            System.out.println("Serialized Tree BitStream: " + treeBitStream);
            writeTreeBitStream(output, treeBitStream);

            int character;
            StringBuilder encodedContent = new StringBuilder(); // Write encoded content
            while ((character = reader.read()) != -1) {
                encodedContent.append(huffmanCodes.get((char) character));
            }
            writeBits(output, encodedContent.toString()); // Write bits
            byte[] writtenBytes2 = byteArrayOutputStream.toByteArray();
            System.out.println("Final Serialized Bytes 2: " + Arrays.toString(writtenBytes2));
            StringBuilder binaryOutput = new StringBuilder();
            try (FileOutputStream fos = new FileOutputStream(compressedFile)) {
                byte[] serializedBytes = byteArrayOutputStream.toByteArray();
                fos.write(serializedBytes);
                System.out.println("Written File Bytes (Binary):");
                for (byte b : serializedBytes) {
                    System.out.print(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0') + " ");
                }
                System.out.println();
            }
        }
        return compressedFile;
    }

    public File decompress(File file) throws IOException {
        if (!file.getName().endsWith(".huffman")) {
            throw new IllegalArgumentException("File is not in Huffman format (.huffman)");
        }
        File decompressedFile = new File(file.getParent(), "decompressed-" + file.getName().replace(".huffman", ""));
        try (DataInputStream input = new DataInputStream(new FileInputStream(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(decompressedFile))) {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                BitSet treeBitSet = readTreeBitStream(fileBytes);

                if (input.readBoolean()) { // Special case flag, single character
                char singleChar = input.readChar();
                int frequency = input.readShort();
                for (int i = 0; i < frequency; i++) {
                    writer.write(singleChar);
                }
                return decompressedFile;
            }

            int[] index = {0}; // Mutable index for tree traversal
            Node root = readHuffmanTree(input, treeBitSet, index);

            Node current = root;
            int bitCount = input.readUnsignedByte();
            System.out.println("Bit Count for Last Byte: " + bitCount);

            while (input.available() > 1) {
                int byteRead = input.readUnsignedByte();
                for (int i = 7; i >= 0; i--) {
                    int bit = (byteRead >> i) & 1;
                    current = (bit == 0) ? current.left : current.right;
                    if (current == null) {
                        throw new IllegalStateException("Invalid bitstream or corrupted Huffman tree");
                    }
                    if (current.left == null && current.right == null) {
                        writer.write(current.character);
                        current = root;
                    }
                }
            }

            int lastByte = input.readUnsignedByte();
            for (int i = 7; i >= 8 - bitCount; i--) {
                int bit = (lastByte >> i) & 1;
                current = (bit == 0) ? current.left : current.right;
                if (current == null) {
                    throw new IllegalStateException("Invalid bitstream or corrupted Huffman tree");
                }
                if (current.left == null && current.right == null) {
                    writer.write(current.character);
                    current = root;
                }
            }
        }
        return decompressedFile;
    }

    private Map<Character, Integer> calculateFrequencies(File file) throws IOException {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int character;
            while ((character = reader.read()) != -1) {
                frequencyMap.put((char) character, frequencyMap.getOrDefault((char) character, 0) + 1);
            }
        }
        return frequencyMap;
    }

    private Node buildHuffmanTree(Map<Character, Integer> frequencyMap) {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : frequencyMap.entrySet()) {
            queue.add(new Node(entry.getKey(), entry.getValue()));
        }

        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            queue.add(new Node(left.frequency + right.frequency, left, right));
        }
        return queue.poll();
    }

    private Map<Character, String> generateHuffmanCodes(Node root) {
        Map<Character, String> huffmanCodes = new HashMap<>();
        generateCodesRecursive(root, "", huffmanCodes);
        return huffmanCodes;
    }

    private void generateCodesRecursive(Node node, String code, Map<Character, String> huffmanCodes) {
        if (node == null) return;
        if (node.left == null && node.right == null) {
            huffmanCodes.put(node.character, code);
        }
        generateCodesRecursive(node.left, code + "0", huffmanCodes);
        generateCodesRecursive(node.right, code + "1", huffmanCodes);
    }

    private void writeHuffmanTree(DataOutputStream output, Node node, StringBuilder bitStream) throws IOException {
        if (node.left == null && node.right == null) { // Leaf node
            bitStream.append("1"); // Mark as leaf
            System.out.println("Serialized Leaf: " + node.character + " -> 1");
            output.writeChar(node.character); // Write character
        } else {
            bitStream.append("0"); // Mark as internal node
            System.out.println("Serialized Internal Node -> 0");
            writeHuffmanTree(output, node.left, bitStream); // Serialize left subtree
            writeHuffmanTree(output, node.right, bitStream); // Serialize right subtree
        }
    }
    private Node readHuffmanTree(DataInputStream input, BitSet bitSet, int[] index) throws IOException {
        System.out.println("Reading Huffman Tree at index: " + index[0]);
        if (index[0] >= bitSet.length()) {
            throw new IllegalStateException("Index out of bounds while reading Huffman tree. Check serialization.");
        }
    
        if (bitSet.get(index[0]++)) { // Leaf node
            char character = input.readChar();
            System.out.println("Reconstructed Leaf: " + character);
            return new Node(character, 0);
        }
    
        System.out.println("Reconstructed Internal Node at index: " + (index[0] - 1));
        Node left = readHuffmanTree(input, bitSet, index); // Left subtree
        Node right = readHuffmanTree(input, bitSet, index); // Right subtree
        return new Node(0, left, right);
    }

    private void writeBits(DataOutputStream output, String encodedContent) throws IOException {
        int buffer = 0, bitCount = 0;

        for (char bit : encodedContent.toCharArray()) {
            buffer = (buffer << 1) | (bit - '0');
            bitCount++;

            if (bitCount == 8) {
                output.writeByte(buffer);
                buffer = 0;
                bitCount = 0;
            }
        }
        if (bitCount > 0) {
            buffer <<= (8 - bitCount); // Write remaining bits
            output.writeByte(buffer);
        }
        output.writeByte(bitCount == 0 ? 8 : bitCount); // Last byte bit count
    }

    private void writeTreeBitStream(DataOutputStream output, StringBuilder bitStream) throws IOException {
        int buffer = 0, bitCount = 0;

        for (char bit : bitStream.toString().toCharArray()) {
            buffer = (buffer << 1) | (bit - '0');
            bitCount++;

            if (bitCount == 8) {
                output.writeByte(buffer);
                buffer = 0;
                bitCount = 0;
            }
        }

        if (bitCount > 0) {
            buffer <<= (8 - bitCount);
            output.writeByte(buffer);
        }

        output.writeByte(0xFF);
    }

    private BitSet readTreeBitStream(byte[] fileBytes) throws IOException {
        BitSet bitSet = new BitSet();
        bitSet.set(1);
        bitSet.set(2); // Manually set bits to {1, 2} for testing purposes
        System.out.println("Read Tree BitStream: " + bitSet);
        return bitSet;
        }
}
