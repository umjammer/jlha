/*
 * Copyright (C) 2001-2002  Michel Ishizuka  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY MICHEL ISHIZUKA ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MICHEL
 * ISHIZUKA BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jp.gr.java_conf.dangan.util.lha;

/**
 * Provides a group of utility functions for static Huffman coding.<br>
 * Huffman codes are restricted to a maximum of 16 bits.<br>
 *
 * <pre>
 * -- revision history --
 * $Log: StaticHuffman.java,v $
 * Revision 1.0  2002/07/24 00:00:00  dangan
 * add to version control
 * [maintenance]
 *     Source cleanup.
 *     Eliminated tabs.
 *     Updated license text.
 * </pre>
 *
 * @author $Author: dangan $
 * @version $Revision: 1.0 $
 */
public class StaticHuffman {

    /**
     * Limitation of Huffman code length due to LHA being created using DOS 16-bit mode.
     */
    public static final int LimitLen = 16;

    /**
     * Default constructor.
     * Cannot be used.
     */
    private StaticHuffman() {
    }

    /**
     * Creates a table of Huffman code bit lengths from a frequency list.
     *
     * @param FreqList frequency list
     * @return table of Huffman code bit lengths
     */
    public static int[] FreqListToLenList(int[] FreqList) {
        /*
         * Arrays that construct the Huffman tree.
         * In the Huffman tree, 0 to FreqList.length - 1 are all leaves,
         * and their node numbers are the codes. Once the tree is completed,
         * TreeCount - 1 becomes the root node.
         * NodeWeight:: holds the weight of that node
         * SmallNode:: holds the node number of the smaller child node
         * LargeNode:: holds the node number of the larger child node
         * TreeCount:: holds the number of valid nodes
         */
        int[] NodeWeight = new int[FreqList.length * 2 - 1];
        int[] SmallNode = new int[FreqList.length * 2 - 1];
        int[] LargeNode = new int[FreqList.length * 2 - 1];
        int TreeCount = FreqList.length;

        /*
         * List storing node numbers of leaves of the Huffman tree in ascending order.
         * Leafs:: the list itself
         * LeafCount:: number of leaves
         */
        int[] Leafs = new int[FreqList.length];
        int LeafCount = 0;

        /*
         * List storing node numbers of non-leaf nodes of the Huffman tree
         * in ascending order.
         * Nodes:: the list itself
         * NodeCount:: number of non-leaf nodes
         */
        int[] Nodes = new int[FreqList.length - 1];
        int NodeCount = 0;

        // Set leaves in the tree,
        // and set only leaves with frequency of 1 or more in Leafs.
        for (int i = 0; i < FreqList.length; i++) {
            NodeWeight[i] = FreqList[i];

            if (0 < FreqList[i])
                Leafs[LeafCount++] = i;
        }

        if (2 <= LeafCount) {
            // Create Huffman tree

            // Sort elements that should become leaves of the Huffman tree.
            StaticHuffman.MergeSort(Leafs, 0, LeafCount - 1, FreqList, new int[(LeafCount / 2) + 1]);

            // Repeatedly connect the two smallest leaves or nodes to a new node,
            // creating up to the root node.
            // This process completes the Huffman tree.
            int LeafIndex = 0;
            int NodeIndex = 0;
            do {
                int small;
                if (NodeCount <= NodeIndex)
                    small = Leafs[LeafIndex++];
                else if (LeafCount <= LeafIndex)
                    small = Nodes[NodeIndex++];
                else if (NodeWeight[Leafs[LeafIndex]] <= NodeWeight[Nodes[NodeIndex]])
                    small = Leafs[LeafIndex++];
                else
                    small = Nodes[NodeIndex++];

                int large;
                if (NodeCount <= NodeIndex)
                    large = Leafs[LeafIndex++];
                else if (LeafCount <= LeafIndex)
                    large = Nodes[NodeIndex++];
                else if (NodeWeight[Leafs[LeafIndex]] <= NodeWeight[Nodes[NodeIndex]])
                    large = Leafs[LeafIndex++];
                else
                    large = Nodes[NodeIndex++];

                int newNode = TreeCount++;
                NodeWeight[newNode] = NodeWeight[small] + NodeWeight[large];
                SmallNode[newNode] = small;
                LargeNode[newNode] = large;
                Nodes[NodeCount++] = newNode;
            } while (NodeIndex + LeafIndex < NodeCount + LeafCount - 1);

            // Create Huffman code length table from the Huffman tree.
            // Create frequency table of Huffman code lengths from the Huffman tree.
            int[] LenFreq = StaticHuffman.HuffmanTreeToLenFreq(SmallNode, LargeNode, TreeCount - 1);

            // Create code length table from the frequency table of Huffman code lengths.
            int[] LenList = new int[FreqList.length];
            LeafIndex = 0;
            for (int len = StaticHuffman.LimitLen; 0 < len; len--)
                while (0 < LenFreq[len]--)
                    LenList[Leafs[LeafIndex++]] = len;

            return LenList;
        } else {
            return new int[FreqList.length];
        }
    }

    /**
     * Creates a table of Huffman code bit lengths from a frequency list.
     * Outputs the same code as original LHA.
     *
     * @param FreqList frequency list
     * @return table of Huffman code bit lengths
     */
    public static int[] FreqListToLenListOriginal(int[] FreqList) {
        /*
         * Arrays that construct the Huffman tree.
         * In the Huffman tree, 0 to FreqList.length - 1 are all leaves,
         * and their node numbers are the codes. Once the tree is completed,
         * TreeCount - 1 becomes the root node.
         * NodeWeight:: holds the weight of that node
         * SmallNode:: holds the node number of the smaller child node
         * LargeNode:: holds the node number of the larger child node
         * TreeCount:: holds the number of valid nodes
         */
        int[] NodeWeight = new int[FreqList.length * 2 - 1];
        int[] SmallNode = new int[FreqList.length * 2 - 1];
        int[] LargeNode = new int[FreqList.length * 2 - 1];
        int TreeCount = FreqList.length;

        /*
         * List storing node numbers of leaves of the Huffman tree in ascending order.
         * Leafs:: the list itself
         * LeafCount:: number of leaves
         */
        int[] Leafs = new int[FreqList.length];
        int LeafCount = 0;

        /*
         * Stores the list of node numbers of all nodes of the Huffman tree
         * in ascending order.
         * Since heap sort is used, Heap[0] is not used.
         * Heap:: the list itself
         * HeapLast:: last element of Heap
         */
        int[] Heap = new int[FreqList.length * 2];
        int HeapLast = 0;

        // Set leaves in the tree,
        // and set only leaves with frequency of 1 or more in Heap.
        for (int i = 0; i < FreqList.length; i++) {
            NodeWeight[i] = FreqList[i];

            if (0 < FreqList[i])
                Heap[++HeapLast] = i;
        }

        if (2 <= HeapLast) {
            // Create Huffman tree

            // Sort elements that should become leaves of the Huffman tree.
            for (int i = HeapLast / 2; 1 <= i; i--)
                StaticHuffman.DownHeap(Heap, HeapLast, NodeWeight, i);

            // Repeatedly connect the two smallest leaves or nodes to a new node,
            // creating up to the root node.
            // This process completes the Huffman tree.
            do {
                int small = Heap[1];
                if (small < FreqList.length)
                    Leafs[LeafCount++] = small;

                Heap[1] = Heap[HeapLast--];
                StaticHuffman.DownHeap(Heap, HeapLast, NodeWeight, 1);
                int large = Heap[1];
                if (large < FreqList.length)
                    Leafs[LeafCount++] = large;

                int newNode = TreeCount++;
                NodeWeight[newNode] = NodeWeight[small] + NodeWeight[large];
                SmallNode[newNode] = small;
                LargeNode[newNode] = large;

                Heap[1] = newNode;
                StaticHuffman.DownHeap(Heap, HeapLast, NodeWeight, 1);
            } while (1 < HeapLast);

            // Create Huffman code length table from the Huffman tree.

            // Create frequency table of Huffman code lengths from the Huffman tree.
            int[] LenFreq = StaticHuffman.HuffmanTreeToLenFreq(SmallNode, LargeNode, TreeCount - 1);
            // Create code length table from the frequency table of Huffman code lengths.
            int[] LenList = new int[FreqList.length];
            int LeafIndex = 0;
            for (int len = StaticHuffman.LimitLen; 0 < len; len--)
                while (0 < LenFreq[len]--)
                    LenList[Leafs[LeafIndex++]] = len;

            return LenList;
        } else {
            return new int[FreqList.length];
        }
    }

    /**
     * Creates a Huffman code table from a list of Huffman code lengths.
     *
     * @param LenList list of Huffman code lengths
     * @return Huffman code table
     * @throws BadHuffmanTableException if Huffman code table cannot be generated because {@code LenList} is invalid
     */
    public static int[] LenListToCodeList(int[] LenList) throws BadHuffmanTableException {
        // Frequency table of Huffman code lengths
        int[] LenFreq = new int[StaticHuffman.LimitLen + 1];
        // Codes corresponding to Huffman code lengths
        int[] CodeStart = new int[StaticHuffman.LimitLen + 2];

        // Create frequency table of Huffman code lengths
        for (int j : LenList) LenFreq[j]++;

        if (LenFreq[0] < LenList.length) {

//           CodeStart[1] = 0; // Commented out because it is not needed in Java.
            for (int i = 1; i <= StaticHuffman.LimitLen; i++)
                CodeStart[i + 1] = CodeStart[i] + LenFreq[i] << 1;

            if (CodeStart[StaticHuffman.LimitLen + 1] != 0x20000)
                throw new BadHuffmanTableException();

            int[] CodeList = new int[LenList.length];
            for (int i = 0; i < CodeList.length; i++)
                if (0 < LenList[i])
                    CodeList[i] = CodeStart[LenList[i]]++;

            return CodeList;
        } else {
            return new int[LenList.length];
        }
    }

    /**
     * Generates a table for Huffman decoding from {@code LenList}.
     *
     * @param LenList table of Huffman code lengths
     * @return table for Huffman decoding
     * @throws BadHuffmanTableException if Huffman code table cannot be generated because {@code LenList} is invalid
     */
    public static short[] createTable(int[] LenList) throws BadHuffmanTableException {
        int[] CodeList = StaticHuffman.LenListToCodeList(LenList);
        int TableBits = 0;
        int LastCode = 0;

        for (int i = 0; i < LenList.length; i++) {
            if (TableBits <= LenList[i]) {
                TableBits = LenList[i];
                LastCode = i;
            }
        }

        short[] Table = new short[1 << TableBits];
        for (int i = 0; i < LenList.length; i++) {
            if (0 < LenList[i]) {
                int start = CodeList[i] << (TableBits - LenList[i]);
                int end = (i != LastCode ? start + (1 << (TableBits - LenList[i])) : Table.length);

                for (int j = start; j < end; j++)
                    Table[j] = (short) i;
            }
        }
        return Table;
    }

    /**
     * Generates a table and tree for Huffman decoding from {@code LenList}.
     * The table has a size of {@code TableBits}, and any portion exceeding that is stored in the tree.<br>
     * The return value is {@code new short[][]{ Table, Tree[0], Tree[1] }}.<br>
     * When looking up the table or traversing the tree, if a negative value is obtained,
     * it is the decoded code with all bits inverted.
     * If it is a positive value, it is an index for traversing the tree,
     * and is used like {@code Tree[bit][index]}.
     *
     * @param LenList   table of Huffman code lengths
     * @param TableBits size of the table for Huffman decoding
     * @return table and tree for Huffman decoding
     * @throws BadHuffmanTableException if Huffman code table cannot be generated because {@code LenList} is invalid
     */
    public static short[][] createTableAndTree(int[] LenList, int TableBits) throws BadHuffmanTableException {

        // Obtain list of Huffman codes from the list of Huffman code lengths.
        int[] CodeList = StaticHuffman.LenListToCodeList(LenList);

        // Traverse the list of Huffman code lengths to obtain LastCode.
        // Also prepare to obtain the array size required to construct the tree.
        short[] Table = new short[1 << TableBits];
        int LastCode = 0;
        for (int i = 0; i < LenList.length; i++) {
            if (LenList[LastCode] <= LenList[i])
                LastCode = i;

            if (TableBits < LenList[i]) {
                Table[CodeList[i] >> (LenList[i] - TableBits)]++;
            }
        }

        // Obtain array size required to construct the tree, and initialize the table.
        final short INIT = -1;
        int count = 0;
        for (int i = 0; i < Table.length; i++) {
            if (0 < Table[i])
                count += Table[i] - 1;
            Table[i] = INIT;
        }
        short[] Small = new short[count];
        short[] Large = new short[count];

        // Construct table and tree.
        int avail = 0;
        for (int i = 0; i < LenList.length; i++) {
            if (0 < LenList[i]) {
                int TreeBits = LenList[i] - TableBits;
                if (TreeBits <= 0) {
                    int start = CodeList[i] << (TableBits - LenList[i]);
                    int end = (i != LastCode ? start + (1 << (TableBits - LenList[i])) : Table.length);
                    for (int j = start; j < end; j++) {
                        Table[j] = (short) ~i;
                    }
                } else {
                    int TableCode = CodeList[i] >> TreeBits;
                    int node;
                    if (Table[TableCode] == INIT) {
                        node = Table[TableCode] = (short) (avail++);
                    } else {
                        node = Table[TableCode];
                    }
                    for (int j = TableBits + 1; j < LenList[i]; j++) {
                        if (0 == (CodeList[i] & (1 << (LenList[i] - j)))) {
                            if (Small[node] == 0)
                                node = Small[node] = (short) (avail++);
                            else
                                node = Small[node];
                        } else {
                            if (Large[node] == 0)
                                node = Large[node] = (short) (avail++);
                            else
                                node = Large[node];
                        }
                    }
                    if (0 == (CodeList[i] & 0x01))
                        Small[node] = (short) ~i;
                    else
                        Large[node] = (short) ~i;
                }
            }
        }
        return new short[][] {
                Table, Small, Large
        };
    }

    /**
     * Merge sort, recursive function.<br>
     * Sorts {@code array} such that in the interval from {@code first} to {@code last},
     * elements are arranged in ascending order of {@code weight}, where {@code array} contains indices into {@code weight}.
     * {@code work} is a work area for that purpose.
     *
     * @param array  array to be sorted
     * @param first  start of sort interval
     * @param last   end of sort interval
     * @param weight list of weights referenced during sorting
     * @param work   work area for merge sort
     */
    private static void MergeSort(int[] array, int first, int last, int[] weight, int[] work) {
        if (first < last) {
            int middle = (first + last) / 2 + (first + last) % 2;
            // Sort first half
            StaticHuffman.MergeSort(array, first, middle - 1, weight, work);
            // Sort second half
            StaticHuffman.MergeSort(array, middle, last, weight, work);

            // Copy first half to work
            System.arraycopy(array, first, work, 0, middle - first);

            // Merge sorted first half and sorted second half while arranging
            int srcIndex = middle;
            int workIndex = 0;
            int dstIndex = first;
            while (srcIndex <= last && workIndex < middle - first)
                array[dstIndex++] = (weight[work[workIndex]] < weight[array[srcIndex]] ? work[workIndex++] : array[srcIndex++]);

            // Restore remaining elements in work to array
            if (workIndex < middle - first)
                System.arraycopy(work, workIndex, array, dstIndex, middle - first - workIndex);
        }
    }

    /**
     * {@code heap} contains indices into {@code weight}.
     * On the premise that a heap is formed at points {@code num*2, num*2+1},
     * creates a heap in {@code heap} with {@code num} as the apex.<br>
     * Part of heap sort.
     *
     * @param heap   array generating the heap
     * @param size   size of the heap
     * @param weight list of weights serving as criteria for ordering
     * @param num    apex of the heap to create this time
     */
    private static void DownHeap(int[] heap, int size, int[] weight, int num) {

        int top = heap[num];
        int i;
        while ((i = 2 * num) <= size) {
            if (i < size && weight[heap[i]] > weight[heap[i + 1]])
                i++;
            if (weight[top] <= weight[heap[i]])
                break;

            heap[num] = heap[i];
            num = i;
        }
        heap[num] = top;
    }

    /**
     * Creates a frequency table of Huffman code lengths from the Huffman tree.<br>
     * Traverses the Huffman tree to create a frequency table of Huffman code lengths.
     * Also performs processing here to restrict code lengths to 16 bits.
     *
     * @param SmallNode table of node numbers of smaller child nodes
     * @param LargeNode table of node numbers of larger child nodes
     * @param root      root node of the Huffman tree
     * @return table of Huffman code lengths with Huffman code lengths restricted to a maximum of 16 bits
     */
    private static int[] HuffmanTreeToLenFreq(int[] SmallNode, int[] LargeNode, int root) {
        int[] LenFreq = new int[StaticHuffman.LimitLen + 1];

        // Create frequency table from Huffman tree
        StaticHuffman.internalHuffmanTreeToLenFreq(SmallNode, LargeNode, root, 0, LenFreq);

//        logger.log(Level.TRACE, "Reached::StaticHuffman.HuffmanTreeToLenFreq--Acquire list of Huffman code lengths from Huffman tree--");

        // If modified due to the maximum 16-bit restriction,
        // correct the code length table by pulling higher nodes down to lower levels
        // from the code length table.
        int weight = 0;
        for (int i = StaticHuffman.LimitLen; 0 < i; i--)
            weight += LenFreq[i] << (StaticHuffman.LimitLen - i);

//        logger.log(Level.TRACE, "weight::" + weight);

        while ((1 << StaticHuffman.LimitLen) < weight) {
            LenFreq[StaticHuffman.LimitLen]--;
            for (int i = StaticHuffman.LimitLen - 1; 0 < i; i--)
                if (0 < LenFreq[i]) {
                    LenFreq[i]--;
                    LenFreq[i + 1] += 2;
                    break;
                }

            weight--;
        }

        return LenFreq;
    }

    /**
     * Huffman tree search method, recursive function.<br>
     * Searches the Huffman tree; if {@code node} is a leaf, updates the passed frequency table of code lengths,
     * and if it is a node, recursively searches both smaller and larger child nodes.
     *
     * @param SmallNode table of node numbers of smaller child nodes
     * @param LargeNode table of node numbers of larger child nodes
     * @param node      node number to process
     * @param len       length from root of the Huffman tree
     * @param LenFreq   frequency table of code lengths
     */
    private static void internalHuffmanTreeToLenFreq(int[] SmallNode, int[] LargeNode, int node, int len, int[] LenFreq) {
        if (node < (SmallNode.length + 1) / 2) {
            // If node is a leaf, update frequency table
            LenFreq[(Math.min(len, StaticHuffman.LimitLen))]++;
        } else {
            // If node is a branch node, recursively search both nodes
            StaticHuffman.internalHuffmanTreeToLenFreq(SmallNode, LargeNode, SmallNode[node], len + 1, LenFreq);
            StaticHuffman.internalHuffmanTreeToLenFreq(SmallNode, LargeNode, LargeNode[node], len + 1, LenFreq);
        }
    }
}
