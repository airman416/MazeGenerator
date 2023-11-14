import javalib.impworld.*;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import javalib.worldimages.*;

//Represents a mutable collection of items
interface ICollection<T> {
  // Is this collection empty?
  boolean isEmpty();

  // EFFECT: adds the item to the collection
  void add(T item);

  // Returns the first item of thecollection
  // EFFECT: removes that first item
  T remove();
}

// represents a Stack data structure
class Stack<T> implements ICollection<T> {
  Deque<T> contents;

  Stack() {
    this.contents = new ArrayDeque<T>();
  }

  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  public T remove() {
    return this.contents.removeFirst();
  }

  public void add(T item) {
    this.contents.addFirst(item);
  }
}

// represents a Queue data structure
class Queue<T> implements ICollection<T> {
  Deque<T> contents;

  Queue() {
    this.contents = new ArrayDeque<T>();
  }

  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  public T remove() {
    return this.contents.removeFirst();
  }

  public void add(T item) {
    this.contents.addLast(item); // NOTE: Different from Stack!
  }
}

// represents a player of the maze
class Player {
  Cell currentPos;
  ArrayList<Cell> visited;

  Player(Cell currentPos) {
    this.currentPos = currentPos;
    this.visited = new ArrayList<Cell>();
    this.visited.add(this.currentPos);
  }
}

// represents a Maze World
class MazeWorld extends World {

  static int SCREEN_WIDTH = 800;
  int screenHeight;
  int mazeWidth;
  int mazeHeight;
  int cellSize;
  int horizontalPreference;

  Graph graph;
  Kruskal kruskal;
  Player player;

  int currentEdgeCount;
  HashMap<Cell, Cell> cameFromEdge;
  Deque<Cell> alreadySeen;
  Stack<Cell> worklistStack;
  Queue<Cell> worklistQueue;
  ArrayList<Cell> backTrackPath;

  boolean mazeGenerating;
  boolean doneSearching;
  Cell currentCell;
  boolean doneAddingNeighbors;
  boolean doneBackTracking;
  boolean dfs;
  boolean bfs;
  boolean toggleAlreadySeen;
  int bFSWrongMoves;
  int dFSWrongMoves;
  int playerMoves;
  int bFSPathLength;
  int dFSPathLength;
  boolean ended;

  MazeWorld(int mazeWidth, int mazeHeight, int horizontalPreference) {
    this.mazeWidth = mazeWidth;
    this.mazeHeight = mazeHeight;
    this.cellSize = Math.min(SCREEN_WIDTH / mazeWidth, 800 / mazeHeight);
    this.screenHeight = this.mazeHeight * this.cellSize;
    this.horizontalPreference = horizontalPreference;
    this.initGraph();    
    this.initKruskal();
  }

  // constructor for testing
  MazeWorld(int mazeWidth, int mazeHeight, int horizontalPreference, int seed) {
    this.mazeWidth = mazeWidth;
    this.mazeHeight = mazeHeight;
    this.cellSize = Math.min(SCREEN_WIDTH / mazeWidth, 800 / mazeHeight);
    this.screenHeight = this.mazeHeight * this.cellSize;
    this.horizontalPreference = horizontalPreference;
    this.initGraph(seed);  
    this.initKruskal();
  }

  // overloads initGraph() by providing a seed to random
  // initializes the graph with cells for each coordinate
  void initGraph(int seed) {
    Random rand = new Random(seed);

    // create all the cells
    ArrayList<Cell> cells = new ArrayList<Cell>();
    for (int x = 0; x < this.mazeHeight; x++) {
      for (int y = 0; y < this.mazeWidth; y++) {
        Cell cell = new Cell(y, x);
        cells.add(cell);
      }
    }

    // create cell outEdges
    for (Cell cell : cells) {
      // connect this cell to the cell to its right
      if (cell.x < this.mazeWidth - 1) {
        Cell rightCell = cells.get(this.mazeWidth * cell.y + cell.x + 1);
        int weight = rand.nextInt(10) + 1; 
        Edge edge1 = new Edge(cell, rightCell, weight);
        Edge edge2 = new Edge(rightCell, cell, weight);
        cell.outEdges.add(edge1);
        rightCell.outEdges.add(edge2);
      }

      // connect this cell to the cell below it
      if (cell.y < this.mazeHeight - 1) {
        Cell bottomCell = cells.get(this.mazeWidth * (cell.y + 1) + cell.x);
        int weight = rand.nextInt(10 * this.horizontalPreference) + 1; 
        Edge edge3 = new Edge(cell, bottomCell, weight);
        Edge edge4 = new Edge(bottomCell, cell, weight);
        cell.outEdges.add(edge3);
        //cell.bottom = edge3.to;
        bottomCell.outEdges.add(edge4);
        //bottomCell.top = edge3.from;
      }
    }

    // adds cells to graph
    this.graph = new Graph(cells);

  }

  // initializes the graph with cells for each coordinate
  void initGraph() {
    Random rand = new Random();

    // create all the cells
    ArrayList<Cell> cells = new ArrayList<Cell>();
    for (int x = 0; x < this.mazeHeight; x++) {
      for (int y = 0; y < this.mazeWidth; y++) {
        Cell cell = new Cell(y, x);
        cells.add(cell);
      }
    }

    // create cell outEdges
    for (Cell cell : cells) {
      // connect this cell to the cell to its right
      if (cell.x < this.mazeWidth - 1) {
        Cell rightCell = cells.get(this.mazeWidth * cell.y + cell.x + 1);
        int weight = rand.nextInt(10) + 1; 
        Edge edge1 = new Edge(cell, rightCell, weight);
        Edge edge2 = new Edge(rightCell, cell, weight);
        cell.outEdges.add(edge1);
        rightCell.outEdges.add(edge2);
      }

      // connect this cell to the cell below it
      if (cell.y < this.mazeHeight - 1) {
        Cell bottomCell = cells.get(this.mazeWidth * (cell.y + 1) + cell.x);
        int weight = rand.nextInt(10 * this.horizontalPreference) + 1; 
        Edge edge3 = new Edge(cell, bottomCell, weight);
        Edge edge4 = new Edge(bottomCell, cell, weight);
        cell.outEdges.add(edge3);
        //cell.bottom = edge3.to;
        bottomCell.outEdges.add(edge4);
        //bottomCell.top = edge3.from;
      }
    }

    // adds cells to graph
    this.graph = new Graph(cells);

  }

  // runs Kruskal's algorithm on this graph
  // and reinitializes several fields for reuse
  void initKruskal() {
    this.currentEdgeCount = 0;
    this.kruskal = new Kruskal(this.graph);
    this.cameFromEdge = new HashMap<Cell, Cell>();
    this.alreadySeen = new ArrayDeque<Cell>();
    this.worklistStack = new Stack<Cell>();
    this.worklistQueue = new Queue<Cell>();
    this.backTrackPath = new ArrayList<Cell>();
    this.currentCell = this.graph.cells.get(this.mazeWidth * this.mazeHeight - 1);
    this.mazeGenerating = true;
    this.doneSearching = false;
    this.doneAddingNeighbors = false;
    this.player = new Player(this.graph.cells.get(0));
    this.dfs = false;
    this.bfs = false;
    this.doneBackTracking = false;
    this.toggleAlreadySeen = true;
    this.bFSWrongMoves = 0;
    this.dFSWrongMoves = 0;
    this.playerMoves = 0;
    this.bFSPathLength = 0;
    this.dFSPathLength = 0;
    this.ended = false;
  }

  // displays the maze
  public WorldScene makeScene() {
    WorldScene toReturn = new WorldScene(SCREEN_WIDTH, screenHeight + 100);

    // displays if the user reaches the end of maze
    if (this.ended) {
      if (!this.doneSearching) {
        this.hasPathBetweenDFSImmediately(this.graph.cells.get(0), 
            this.graph.cells.get(this.mazeHeight * this.mazeWidth - 1));
      }

      toReturn.placeImageXY(new TextImage("You win!", SCREEN_WIDTH / 8, Color.black), 
          SCREEN_WIDTH / 2, screenHeight / 2 - 35);
      toReturn.placeImageXY(new TextImage("With " + Integer.toString(this.playerMoves 
          - this.backTrackPath.size()) + " wrong moves!",
          SCREEN_WIDTH / 15, Color.black), SCREEN_WIDTH / 2, screenHeight / 2 + 35);
      return toReturn;
    }

    // places where BFS/DFS searches
    if (!this.alreadySeen.isEmpty() && !this.mazeGenerating && this.toggleAlreadySeen) {
      for (Cell cell : this.alreadySeen) {
        toReturn.placeImageXY(cell.makeLightBlueCell(cellSize),
            cell.x * cellSize + cellSize / 2,
            cell.y * cellSize + cellSize / 2);
      }
    }

    // places cell walls
    for (Cell cell : this.graph.cells) {
      if (cell.x == 0 && cell.y == 0) {
        toReturn.placeImageXY(cell.makeStartCell(cellSize), 
            cell.x * cellSize + cellSize / 2,
            cell.y * cellSize + cellSize / 2);
        toReturn.placeImageXY(cell.makeCell(cellSize, true, true, Color.black), 
            cell.x * cellSize + cellSize / 2,
            cell.y * cellSize + cellSize / 2);
      } else if (cell.x == mazeWidth - 1 && cell.y == mazeHeight - 1) {
        toReturn.placeImageXY(cell.makeEndCell(cellSize), 
            cell.x * cellSize + cellSize / 2,
            cell.y * cellSize + cellSize / 2);
      } else {
        toReturn.placeImageXY(cell.makeCell(cellSize, true, true, Color.black), 
            cell.x * cellSize + cellSize / 2,
            cell.y * cellSize + cellSize / 2);
      }
    }

    // removes cell walls
    for (Edge edge : this.kruskal.edgesInTree) {
      if (edge.to.x == edge.from.x + 1) {
        toReturn.placeImageXY(edge.from.makeCell(cellSize, false, true, Color.white), 
            edge.from.x * cellSize + cellSize / 2,
            edge.from.y * cellSize + cellSize / 2);
      }
      if (edge.to.y == edge.from.y + 1) {
        toReturn.placeImageXY(edge.from.makeCell(cellSize, true, false, Color.white), 
            edge.from.x * cellSize + cellSize / 2,
            edge.from.y * cellSize + cellSize / 2);
      }
    }

    // displays player movement
    Cell currentPlayerCell = this.player.currentPos;
    for (Cell cell : this.player.visited) {
      toReturn.placeImageXY(cell.makeLightBlueCell(cellSize),
          cell.x * cellSize + cellSize / 2,
          cell.y * cellSize + cellSize / 2);
    }
    toReturn.placeImageXY(currentPlayerCell.makeBlackCell(cellSize),
        currentPlayerCell.x * cellSize + cellSize / 2,
        currentPlayerCell.y * cellSize + cellSize / 2);

    // displays backtracking when search finishes
    if (this.doneSearching) {
      for (Cell cell : this.backTrackPath) {
        toReturn.placeImageXY(cell.makeDarkBlueCell(cellSize),
            cell.x * cellSize + cellSize / 2,
            cell.y * cellSize + cellSize / 2);
      }
      toReturn.placeImageXY(this.graph.cells.get(0).makeDarkBlueCell(cellSize),
          0 * cellSize + cellSize / 2,
          0 * cellSize + cellSize / 2);
    }

    // outline around the window
    toReturn.placeImageXY(new RectangleImage(SCREEN_WIDTH, screenHeight, 
        OutlineMode.OUTLINE, Color.black), 
        SCREEN_WIDTH / 2, screenHeight / 2);

    // displays BFS/DFS statistics
    toReturn.placeImageXY(new TextImage("BFS: " + Integer.toString(
        this.bFSWrongMoves - this.bFSPathLength), 50, Color.black),
        SCREEN_WIDTH / 4, screenHeight + 50);

    toReturn.placeImageXY(new TextImage("DFS: " + Integer.toString(
        this.dFSWrongMoves - this.dFSPathLength), 50, Color.black),
        3 * SCREEN_WIDTH / 4, screenHeight + 50);

    return toReturn;
  }

  // reconstructs path from DFS/BFS
  void backtrackPath(Cell from, Cell to) {
    if (this.currentCell != from) {
      this.currentCell = this.cameFromEdge.get(this.currentCell);
      if (this.currentCell == null) {
        return;
      }
      this.backTrackPath.add(this.currentCell);
      if (this.dfs && !this.bfs) {
        this.dFSPathLength++;
      } else {
        this.bFSPathLength++;        
      }
    } else {
      this.doneBackTracking = true;
      return;
    }

  }

  // runs backtrack immediately rather than on every tick
  // (used to find how many wrong moves a user made by 
  // comparing to the actual answer)
  void backtrackPathImmediately(Cell from, Cell to) {
    for (this.currentCell = to; !this.currentCell.equals(from); 
        this.currentCell = this.cameFromEdge.get(this.currentCell)) {
      if (this.currentCell == null) {
        continue;
      }
      this.backTrackPath.add(this.currentCell);
    }
  }

  // finds edges in tree starting from this cell
  ArrayList<Edge> edgesFromCell(Cell cell) {
    ArrayList<Edge> toReturn = new ArrayList<Edge>();
    for (Edge edge : this.kruskal.edgesInTree) {
      if (edge.from.equals(cell)) {
        toReturn.add(edge);
      }
    }
    return toReturn;
  }

  //finds edges in tree going to this cell
  ArrayList<Edge> edgesToCell(Cell cell) {
    ArrayList<Edge> toReturn = new ArrayList<Edge>();
    for (Edge edge : this.kruskal.edgesInTree) {
      if (edge.to.equals(cell)) {
        toReturn.add(edge);
      }
    }
    return toReturn;
  }

  // adds the directions you can go from each cell
  void addDirectionsToCell(Cell cell) {
    ArrayList<Edge> edgesInTreeCopy = new ArrayList<Edge>();
    for (Edge edge : this.kruskal.edgesInTree) {
      edgesInTreeCopy.add(edge);
    }

    for (Iterator<Edge> iterator = edgesInTreeCopy.iterator(); iterator.hasNext();) {
      Edge edge = iterator.next();
      if (edge.from.x == cell.x - 1 && edge.from.y == cell.y 
          && this.canGoToCell(edge.from, cell)) {
        cell.left = edge.from;
      }
      else if (edge.from.y == cell.y - 1 && edge.from.x == cell.x 
          && this.canGoToCell(edge.from, cell)) {
        cell.top = edge.from;
      }
      else if (edge.to.x == cell.x + 1 && edge.to.y == cell.y
          && this.canGoToCell(edge.to, cell)) {
        cell.right = edge.to;
      }
      else if (edge.to.y == cell.y + 1 && edge.to.x == cell.x
          && this.canGoToCell(edge.to, cell)) {
        cell.bottom = edge.to;
      }
    }
  }

  //finds the path between two given cells using depth-first search
  boolean hasPathBetweenDFS(Cell from, Cell to) {

    // As long as the worklist isn't empty...
    if (!this.worklistStack.isEmpty()) {
      Cell next = this.worklistStack.remove();
      if (next.equals(to)) {
        this.doneSearching = true;
        this.backtrackPath(from, to);
        return true; // Success!
      }
      else if (this.alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        for (Cell n : next.neighbors) {
          if (this.alreadySeen.contains(n)) {
            continue;
          }
          this.worklistStack.add(n);
          this.cameFromEdge.put(n, next);
        }
        // add next to alreadySeen, since we're done with it
        this.alreadySeen.addFirst(next);
        this.dFSWrongMoves++;
      }
    }
    // We haven't found the to vertex, and there are no more to try
    return false;
  }


  // runs DFS immediately rather than on every tick
  boolean hasPathBetweenDFSImmediately(Cell from, Cell to) {

    this.alreadySeen = new ArrayDeque<Cell>();
    this.worklistStack = new Stack<Cell>();
    this.cameFromEdge = new HashMap<Cell, Cell>();

    this.worklistStack.add(from);
    // Initialize the worklist with the from vertex

    // As long as the worklist isn't empty...
    while (!this.worklistStack.isEmpty()) {
      Cell next = this.worklistStack.remove();
      if (next.equals(to)) {
        this.doneSearching = true;
        this.backtrackPathImmediately(from, to);
        return true; // Success!
      }
      else if (this.alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        for (Cell n : next.neighbors) {
          if (this.alreadySeen.contains(n)) {
            continue;
          }
          this.worklistStack.add(n);
          this.cameFromEdge.put(n, next);
        }
        // add next to alreadySeen, since we're done with it
        this.alreadySeen.addFirst(next);
        this.dFSWrongMoves++;
      }
    }
    // We haven't found the to vertex, and there are no more to try
    return false;
  }

  // finds the path between two given cells using breadth-first search
  boolean hasPathBetweenBFS(Cell from, Cell to) {
    // Initialize the worklist with the from vertex
    this.worklistQueue.add(from);
    // As long as the worklist isn't empty...
    if (!this.worklistQueue.isEmpty()) {
      Cell next = this.worklistQueue.remove();
      if (next.equals(to)) {
        this.doneSearching = true;
        this.backtrackPath(from, to);
        return true; // Success!
      }
      else if (this.alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        for (Cell n : next.neighbors) {
          if (this.alreadySeen.contains(n)) {
            continue;
          }
          this.worklistQueue.add(n);
          this.cameFromEdge.put(n, next);
        }
        // add next to alreadySeen, since we're done with it
        this.alreadySeen.addFirst(next);
        this.bFSWrongMoves++;
      }
    }
    // We haven't found the to vertex, and there are no more to try
    return false;
  }

  // runs this code every tick
  public void onTick() {
    if (this.player.currentPos.equals(this.graph.cells.get(
        this.mazeWidth * this.mazeHeight - 1))) {
      this.ended = true;
    }

    if (this.mazeGenerating) {
      this.currentEdgeCount++;
      this.mazeGenerating = this.kruskal.runKruskal(this.currentEdgeCount - 1);
    } else {
      if (!this.doneAddingNeighbors) {
        this.addToCellNeighbors();
      }
    }

    if (!this.doneSearching && this.dfs && !this.bfs) {
      this.hasPathBetweenDFS(this.graph.cells.get(0),
          this.graph.cells.get(this.mazeHeight * this.mazeWidth - 1));
    }

    if (!this.doneSearching && this.bfs && !this.dfs) {
      this.hasPathBetweenBFS(this.graph.cells.get(0),
          this.graph.cells.get(this.mazeHeight * this.mazeWidth - 1));
    }

    if (this.doneSearching && !this.doneBackTracking) {
      this.backtrackPath(this.graph.cells.get(this.mazeHeight 
          * this.mazeWidth - 1), this.graph.cells.get(0));
    }
  }

  // adds the directions of each cell to its neighbors field
  void addToCellNeighbors() {
    for (Cell cell : this.graph.cells) {
      this.addDirectionsToCell(cell);
    }
    for (Cell cell : this.graph.cells) {
      if (cell.right != null) {
        cell.neighbors.add(cell.right);
      }
      if (cell.left != null) {
        cell.neighbors.add(cell.left);
      }
      if (cell.bottom != null) {
        cell.neighbors.add(cell.bottom);
      }
      if (cell.top != null) {
        cell.neighbors.add(cell.top);
      }
    }
    this.doneAddingNeighbors = true;
  }

  // returns whether a you can go from a given cell to a given cell
  boolean canGoToCell(Cell from, Cell to) {
    for (Edge e : this.kruskal.edgesInTree) {
      if (e.equals(new Edge(from, to, e.weight)) 
          || e.equals(new Edge(to, from, e.weight))) {
        return true;
      }
    }
    return false;
  }

  // handles user key input
  public void onKeyEvent(String key) {
    // make new maze
    if (key.equals("r")) {
      this.initGraph();
      this.initKruskal();
      this.ended = false;
    }
    // move player down
    else if (key.equals("down") && this.canGoToCell(this.player.currentPos, 
        new Cell(this.player.currentPos.x, this.player.currentPos.y + 1))) {
      this.player.visited.add(this.player.currentPos);
      this.player.currentPos = new Cell(this.player.currentPos.x, 
          this.player.currentPos.y + 1);
      this.playerMoves++;
    }
    // move player up
    else if (key.equals("up") && this.canGoToCell(this.player.currentPos, 
        new Cell(this.player.currentPos.x, this.player.currentPos.y - 1))) {
      this.player.visited.add(this.player.currentPos);
      this.player.currentPos = new Cell(this.player.currentPos.x, 
          this.player.currentPos.y - 1);
      this.playerMoves++;
    }
    // move player right
    else if (key.equals("right") && this.canGoToCell(this.player.currentPos, 
        new Cell(this.player.currentPos.x + 1, this.player.currentPos.y))) {
      this.player.visited.add(this.player.currentPos);
      this.player.currentPos = new Cell(this.player.currentPos.x + 1, 
          this.player.currentPos.y);
      this.playerMoves++;
    }
    // move player left
    else if (key.equals("left") && this.canGoToCell(this.player.currentPos, 
        new Cell(this.player.currentPos.x - 1, this.player.currentPos.y))) {
      this.player.visited.add(this.player.currentPos);
      this.player.currentPos = new Cell(this.player.currentPos.x - 1, 
          this.player.currentPos.y);
      this.playerMoves++;
    }
    // runs breadth-first search
    else if (key.equals("b") && !this.mazeGenerating) {
      this.doneSearching = false;
      this.doneBackTracking = false;
      this.cameFromEdge = new HashMap<Cell, Cell>();
      this.currentCell = this.graph.cells.get(this.mazeWidth * this.mazeHeight - 1);
      this.alreadySeen = new ArrayDeque<Cell>();
      this.worklistQueue = new Queue<Cell>();
      this.worklistStack = new Stack<Cell>();
      this.backTrackPath = new ArrayList<Cell>();
      this.bfs = true;
      this.dfs = false;
      this.bFSWrongMoves = 0;
      this.bFSPathLength = 0;
    }
    // runs depth-first search
    else if (key.equals("d") && !this.mazeGenerating) {
      this.doneSearching = false;
      this.doneBackTracking = false;
      this.cameFromEdge = new HashMap<Cell, Cell>();
      this.currentCell = this.graph.cells.get(this.mazeWidth * this.mazeHeight - 1);
      this.alreadySeen = new ArrayDeque<Cell>();
      this.worklistQueue = new Queue<Cell>();
      this.worklistStack = new Stack<Cell>();
      this.worklistStack.add(this.graph.cells.get(0));
      this.backTrackPath = new ArrayList<Cell>();
      this.dfs = true;
      this.bfs = false;
      this.dFSWrongMoves = 0;
      this.dFSPathLength = 0;
    }
    // toggles viewing of nodes visited by search
    else if (key.equals("t")) {
      this.toggleAlreadySeen = !this.toggleAlreadySeen;
    }
    // skips animation of maze generation
    else if (key.equals("s")) {
      this.kruskal.runKruskalImmediately();
      this.mazeGenerating = false;
    }
  }
}