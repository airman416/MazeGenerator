# Maze Generator
Generates a maze uses Kruskal's algorithm. Allows gameplay and path-finding using breadth-first search and depth-first search.

## Controls:
    Press "r" to generate a new maze and start a new game.
    Use the arrow keys (up, down, left, right) to move your player through the maze.
    Press "b" to run Breadth-First Search to solve the maze.
    Press "d" to run Depth-First Search to solve the maze.
    Press "t" to toggle the display of nodes visited by the search algorithms.
    Press "s" to skip the maze generation animation.

The MazeWorld constructor requires 3 arguments: mazeWidth, mazeHeight, and horizontalPreference.
The higher horizontalPreference, the more horizontally straight the mazes will be. The lower it is (less than 1), the more vertically straight mazes will be.

