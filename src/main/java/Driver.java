// tests methods in the MazeWorld class
class Driver {

    // displays the maze for the user
    public static void main(String args[]) {
        MazeWorld one = new MazeWorld(20, 15, 1);
        one.bigBang(one.SCREEN_WIDTH, one.screenHeight + 100, 0.0005);
    }
}