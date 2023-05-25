// index.js
"use strict";
const LinkedList = require('./LinkedList.js');
const Node = require('./Node.js');
//Access HeadNode => this.head
//Check if list is empty => this.isEmpty() 
//Node class  { data ; Node nextElement;}

//Inserts a value at the end of the list  
LinkedList.prototype.insertAtTail = function(newData) {

  //Creating a new Node with value data
  let node = new Node(newData);

  //check for case when list is empty
	if (this.isEmpty()) {
		//Needs to Insert the new node at Head
		this.head = node; 
		return this; 
	}

	//Start from head
	let currentNode = this.head;

	//Iterate to the last element
	while (currentNode.nextElement != null) {
		currentNode = currentNode.nextElement; 
	}

	//Make new node the nextElement of last node of list
	currentNode.nextElement = node; 
  return this;
}

let list = new LinkedList();
for (let i = 0; i < 5; i++) {
	list.insertAtTail(i);
	list.printList(); 
}