package dev.gregross.challenges.compress

fun generateCodeTable(root: HuffmanNode): Map<Char, String> {
    val codes = mutableMapOf<Char, String>()
    buildCodes(root, "", codes)
    return codes
}

private fun buildCodes(node: HuffmanNode, prefix: String, codes: MutableMap<Char, String>) {
    when (node) {
        is HuffmanNode.Leaf -> codes[node.char] = prefix.ifEmpty { "0" }
        is HuffmanNode.Branch -> {
            buildCodes(node.left, prefix + "0", codes)
            buildCodes(node.right, prefix + "1", codes)
        }
    }
}
