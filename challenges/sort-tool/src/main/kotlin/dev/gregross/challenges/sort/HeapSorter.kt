package dev.gregross.challenges.sort

class HeapSorter : Sorter {
    override fun sort(lines: List<String>): List<String> {
        val arr = lines.toMutableList()
        val n = arr.size

        // Build max heap
        for (i in n / 2 - 1 downTo 0) {
            siftDown(arr, n, i)
        }

        // Extract elements from heap one by one
        for (i in n - 1 downTo 1) {
            val temp = arr[0]
            arr[0] = arr[i]
            arr[i] = temp
            siftDown(arr, i, 0)
        }

        return arr
    }

    private fun siftDown(arr: MutableList<String>, heapSize: Int, root: Int) {
        var largest = root
        val left = 2 * root + 1
        val right = 2 * root + 2

        if (left < heapSize && arr[left] > arr[largest]) {
            largest = left
        }
        if (right < heapSize && arr[right] > arr[largest]) {
            largest = right
        }

        if (largest != root) {
            val temp = arr[root]
            arr[root] = arr[largest]
            arr[largest] = temp
            siftDown(arr, heapSize, largest)
        }
    }
}
