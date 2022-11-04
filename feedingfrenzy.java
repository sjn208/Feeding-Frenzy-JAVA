import java.awt.Color;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javalib.funworld.World;
import javalib.funworld.WorldScene;
import javalib.worldimages.BesideImage;
import javalib.worldimages.EllipseImage;
import javalib.worldimages.EquilateralTriangleImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.RotateImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldEnd;
import tester.Tester;

/* FEEDING FRENZY GAME, Jasmine Sajna
 * 
 * This program generates a simplified version of the popular "Feeding Frenzy" game
 * PREMISE: a player (represented by an orange fish) should move via arrow keys 
 *          and must "eat" randomly placed smaller fish to grow in size
 * TO WIN: Be the largest fish on the screen (size is determined by the height of the fish)
 * GAME OVER: Don't get eaten by the larger fish!
 */

// abstract class to represent a fish
abstract class AFish {
  int width;
  int height; // HEIGHT is size indicator!
  Color c;
  int x;
  int y;

  AFish(int width, int height, Color c) {
    Random rand = new Random();
    this.width = width;
    this.height = height;
    this.c = c;
    this.x = rand.nextInt(FishWorld.GAME_LENGTH);
    this.y = rand.nextInt(FishWorld.GAME_HEIGHT);
  }

  // for testing randomness, passes a seed as well
  AFish(int width, int height, Color c, int seed) {
    this.width = width;
    this.height = height;
    this.c = c;
    Random rand2 = new Random(seed);
    this.x = rand2.nextInt(FishWorld.GAME_LENGTH);
    this.y = rand2.nextInt(FishWorld.GAME_HEIGHT);
  }

  AFish(int width, int height, Color c, int x, int y) {
    this.width = width;
    this.height = height;
    this.c = c;
    this.x = x;
    this.y = y;
  }

  // draws the fish as an ellipse and triangle (rotated 35 degrees)
  public WorldScene draw(WorldScene acc) {
    return acc
        .placeImageXY(
            new BesideImage(new EllipseImage(this.width, this.height, OutlineMode.SOLID, this.c),
                new RotateImage(
                    new EquilateralTriangleImage(this.height, OutlineMode.SOLID, this.c), 35)),
            this.x, this.y);
  }

  // keeps the position values on the screen itself (re-enters from sides when
  // moving past)
  public int validPos(int pos, int bound) {
    if (pos > bound) {
      return pos - bound;
    }
    else if (pos < 0) {
      return pos + bound;
    }
    else {
      return pos;
    }
  }

  // combines the width of two fish (helper method for determining collision)
  public double widthCombination(AFish a1) {
    return ((this.width + a1.width) / 2.0);
  }

  // find distance between the center of two fish
  public double distance(AFish a1) {
    return Math.sqrt(Math.pow(this.x - a1.x, 2) + Math.pow(this.y - a1.y, 2));
  }

  // is this fish bigger than that fish?
  public boolean biggerThan(AFish a1) {
    return (this.height > a1.height);
  }
}

// class to represent the fish in the background
class Fish extends AFish {
  int direction;

  Fish(int width, int height, Color c) {
    super(width, height, c);
    Random rand = new Random();

    // if the random value is even, the fish goes left across the screen
    int temp = rand.nextInt(2);
    if (temp % 2 == 0) {
      this.direction = -1;
    }
    else { // goes right across the screen
      this.direction = 1;
    }
  }

  Fish(int width, int height, Color c, int dir, int seed) {
    super(width, height, c, seed);
    this.direction = dir;
  }

  Fish(int width, int height, Color c, int x, int y, int dir) {
    super(width, height, c, x, y);
    this.direction = dir;
  }

  // moves the fish in its direction
  public Fish move() {
    // validPos is called when creating the x position so that the fish can wrap
    // around the screen
    return new Fish(this.width, this.height, this.c,
        validPos(this.x + direction, FishWorld.GAME_LENGTH), this.y, this.direction);
  }

}

//a class to represent a player fish
class PlayerFish extends AFish {

  PlayerFish(int width, int height, int x, int y) {
    super(width, height, Color.orange, x, y);
  }

  // create a new dot that is like this PlayerFish but is shifted on the x-axis
  public PlayerFish move(int xmove, int ymove) {
    int newx = validPos(this.x + xmove, FishWorld.GAME_LENGTH);
    int newy = validPos(this.y + ymove, FishWorld.GAME_HEIGHT);
    return new PlayerFish(this.width, this.height, newx, newy);
  }

  // grow the player if they correctly collide with a larger fish in the list of
  // fishes given
  public PlayerFish grow(IList<Fish> fishes) {
    // TRANSLATION: if there is a collision in the list of fishes smaller than
    // this...
    if (fishes.filter(new SmallerThan(this.height)).foldr(new CollisionCheck(this), false)) {
      return new PlayerFish(this.width + 3, this.height + 3, this.x, this.y);
    }
    else {
      return this;
    }
  }

}

// to represent a list of T (Template List)
interface IList<T> {

  // filter this list by the given predicate
  IList<T> filter(Predicate<T> pred);

  // maps a function onto each member of the list, producing a list of the results
  <U> IList<U> map(Function<T, U> fun);

  // combines the items in this list using the given function
  <U> U foldr(BiFunction<T, U, U> fun, U base);
}

// to represent an empty list of T
class MtList<T> implements IList<T> {

  // filter this list by the given predicate
  public IList<T> filter(Predicate<T> pred) {
    return this;
  }

  // maps a function onto each member of the list, producing a list of the results
  public <U> IList<U> map(Function<T, U> fun) {
    return new MtList<U>();
  }

  // combines the items in this list using the given function
  public <U> U foldr(BiFunction<T, U, U> fun, U base) {
    return base;
  }
}

// to represent a non empty list of T
class ConsList<T> implements IList<T> {
  T first;
  IList<T> rest;

  ConsList(T first, IList<T> rest) {
    this.first = first;
    this.rest = rest;
  }

  // filter this list by the given predicate
  public IList<T> filter(Predicate<T> pred) {
    if (pred.test(this.first)) {
      return new ConsList<T>(this.first, this.rest.filter(pred));
    }
    else {
      return this.rest.filter(pred);
    }
  }

  // maps a function onto each member of the list, producing a list of the results
  public <U> IList<U> map(Function<T, U> fun) {
    return new ConsList<U>(fun.apply(this.first), this.rest.map(fun));
  }

  // combines the items in this list using the given function
  public <U> U foldr(BiFunction<T, U, U> fun, U base) {
    return fun.apply(this.first, this.rest.foldr(fun, base));
  }

}

// bigger than player predicate for fish list
class BiggerThan implements Predicate<Fish> {
  int playerHeight;

  BiggerThan(int height) {
    this.playerHeight = height;
  }

  // check if this fish is bigger than the player
  public boolean test(Fish backgroundF) {
    return (backgroundF.height > this.playerHeight);
  }
}

// collides with player bifunct for fish list
class CollisionCheck implements BiFunction<Fish, Boolean, Boolean> {
  PlayerFish pf;

  CollisionCheck(PlayerFish pf) {
    this.pf = pf;
  }

  // check if this fish collided with the player or if the previous has
  public Boolean apply(Fish fsh, Boolean prevIsCollided) {
    return prevIsCollided || (fsh.distance(this.pf) <= fsh.widthCombination(pf));
  }
}

// does not collide predicate for fish list
class NotCollided implements Predicate<Fish> {
  PlayerFish pf;

  NotCollided(PlayerFish pf) {
    this.pf = pf;
  }

  // has this fish not collided with the player
  public boolean test(Fish backgroundF) {
    return (backgroundF.distance(this.pf) > backgroundF.widthCombination(pf))
        || backgroundF.biggerThan(pf);
  }
}

// smaller than predicate for fish list 
class SmallerThan implements Predicate<Fish> {
  int playerHeight;

  SmallerThan(int height) {
    this.playerHeight = height;
  }

  // check if this fish is smaller than the player
  public boolean test(Fish backgroundF) {
    return (backgroundF.height < this.playerHeight);
  }
}

// all smaller bifunction for fish list
class AllSmaller implements BiFunction<Fish, Boolean, Boolean> {
  PlayerFish p1;

  AllSmaller(PlayerFish p1) {
    this.p1 = p1;
  }

  // determine if all fish are smaller than the player
  public Boolean apply(Fish fsh, Boolean prevIsSmaller) {
    return prevIsSmaller && !(fsh.biggerThan(p1));
  }
}

// move fish function for list of fish
class MoveFish implements Function<Fish, Fish> {
  // move the fish
  public Fish apply(Fish x) {
    return x.move();
  }
}

// draw fish bifunct for list fish
class DrawFish implements BiFunction<Fish, WorldScene, WorldScene> {
  // draw the list of fish
  public WorldScene apply(Fish fsh, WorldScene w) {
    return fsh.draw(w);
  }

}

// represent a world/game of fish- player and background
class FishWorld extends World {
  final public static int GAME_HEIGHT = 400;
  final public static int GAME_LENGTH = 600;
  PlayerFish user;
  IList<Fish> fishes;

  FishWorld(PlayerFish usr, IList<Fish> fishes) {
    this.user = usr;
    this.fishes = fishes;
  }

  // draw the scene
  public WorldScene makeScene() {
    return this.user
        .draw(this.fishes.foldr(new DrawFish(), new WorldScene(GAME_LENGTH, GAME_HEIGHT)));
  }

  // key event up
  public FishWorld onKeyEvent(String key) {
    if (key.equals("up")) {
      return new FishWorld(this.user.move(0, -10), this.fishes);
    }
    else if (key.equals("down")) {
      return new FishWorld(this.user.move(0, 10), this.fishes);
    }
    else if (key.equals("right")) {
      return new FishWorld(this.user.move(10, 0), this.fishes);
    }
    else if (key.equals("left")) {
      return new FishWorld(this.user.move(-10, 0), this.fishes);
    }
    else {
      return this;
    }

  }

  // onTick, the game should move the background fishes, grow the player if
  // necessary, and remove eaten fish
  public FishWorld onTick() {
    return this.moveFishes().growPlayer().removeSmallerCollided();
  }

  // shift all fish in their direction
  public FishWorld moveFishes() {
    return new FishWorld(this.user, this.fishes.map(new MoveFish()));
  }

  // keep all fish that are not appropriately collided with
  public FishWorld removeSmallerCollided() {
    return new FishWorld(this.user, this.fishes.filter(new NotCollided(this.user)));
  }

  // grow the player (only occurs when player collides with smaller fish)
  public FishWorld growPlayer() {
    return new FishWorld(this.user.grow(this.fishes), this.fishes);
  }

  // end game if all fishes are smaller or if player has been eaten by a larger
  // fish
  @Override
  public WorldEnd worldEnds() {

    // check if all fishes are smaller
    if (this.fishes.foldr(new AllSmaller(this.user), true)) {
      return new WorldEnd(true, this.showWin(true));
    }
    // check if there is collision with the list of only the bigger fish
    else if (this.fishes.filter(new BiggerThan(this.user.height))
        .foldr(new CollisionCheck(this.user), false)) {
      return new WorldEnd(true, this.showWin(false));
    }
    // world does not end (first parameter is false)
    else {
      return new WorldEnd(false, this.showWin(false));
    }
  }

  // end screen win or loss
  public WorldScene showWin(boolean isWin) {
    WorldScene endScreen = new WorldScene(600, 400);

    if (!isWin) {
      return endScreen.placeImageXY(new TextImage("You Lost!", Color.RED), GAME_LENGTH / 2,
          GAME_HEIGHT / 2);
    }
    else {
      return endScreen.placeImageXY(new TextImage("You Won!", Color.GREEN), GAME_LENGTH / 2,
          GAME_HEIGHT / 2);
    }
  }

}

// examples class to test all methods of the program
class ExamplesFrenzy {
  PlayerFish p1 = new PlayerFish(25, 10, FishWorld.GAME_LENGTH / 2, FishWorld.GAME_HEIGHT / 2);
  PlayerFish p1Up = new PlayerFish(25, 10, 300, 190);
  PlayerFish p1Down = new PlayerFish(25, 10, 300, 210);
  PlayerFish p1Left = new PlayerFish(25, 10, 290, 200);
  PlayerFish p1Right = new PlayerFish(25, 10, 310, 200);
  PlayerFish p1Grow = new PlayerFish(28, 13, 300, 200);
  Fish b1 = new Fish(20, 8, Color.GREEN);
  Fish b2 = new Fish(20, 8, Color.GREEN);
  Fish b3 = new Fish(20, 8, Color.GREEN);
  Fish b4 = new Fish(20, 15, Color.RED);
  Fish b5 = new Fish(20, 19, Color.RED);
  Fish b6 = new Fish(20, 8, Color.GREEN, 100, 200, 1);
  Fish b7 = new Fish(20, 15, Color.RED, 200, 400, -1);
  Fish b8 = new Fish(20, 8, Color.GREEN, 101, 200, 1);
  Fish b9 = new Fish(20, 15, Color.RED, 199, 400, -1);
  Fish b10 = new Fish(20, 8, Color.RED, 500, 200, 1);
  Fish b11 = new Fish(20, 8, Color.GREEN, 300, 300, -1);
  Fish b12 = new Fish(20, 8, Color.GREEN, 300, 200, 1);

  IList<Fish> mt = new MtList<Fish>();
  IList<Fish> listfish = new ConsList<Fish>(b5, new ConsList<Fish>(b4,
      new ConsList<Fish>(b3, new ConsList<Fish>(b2, new ConsList<Fish>(b1, mt)))));
  IList<Fish> listfish2 = new ConsList<Fish>(b6,
      new ConsList<Fish>(b7, new ConsList<Fish>(b10, new ConsList<Fish>(b11, mt))));
  IList<Fish> listfish3 = new ConsList<Fish>(b6,
      new ConsList<Fish>(b7, new ConsList<Fish>(b10, mt)));
  WorldScene w1 = new WorldScene(600, 400);

  IList<Fish> listfish4 = new ConsList<Fish>(b6, new ConsList<Fish>(b7, mt));
  IList<Fish> listfish5 = new ConsList<Fish>(b8, new ConsList<Fish>(b9, mt));
  IList<Fish> listfish6 = new ConsList<Fish>(b12,
      new ConsList<Fish>(b6, new ConsList<Fish>(b7, mt)));
  FishWorld world2 = new FishWorld(this.p1, this.listfish4);
  FishWorld world3 = new FishWorld(this.p1, this.listfish5);
  FishWorld world4 = new FishWorld(this.p1, this.listfish6);
  FishWorld world5 = new FishWorld(this.p1Grow, this.listfish6);

  boolean testBigBang(Tester t) {
    FishWorld world = new FishWorld(this.p1, this.listfish);
    double tickRate = .1;
    return world.bigBang(FishWorld.GAME_LENGTH, FishWorld.GAME_HEIGHT, tickRate);
  }

  // test randomness of fish constructor
  boolean testRandomConst(Tester t) {
    Fish f1 = new Fish(25, 10, Color.GREEN, -1, 3);
    Fish f2 = new Fish(35, 19, Color.GREEN, -1, 8);
    return t.checkExpect(f1.x, 134) && t.checkExpect(f1.y, 60) && t.checkExpect(f2.x, 364)
        && t.checkExpect(f2.y, 156);
  }

  // test draw of a fish, and a playerfish
  boolean testDraw(Tester t) {
    return t.checkExpect(this.p1.draw(w1),
        w1.placeImageXY(
            new BesideImage(new EllipseImage(25, 10, OutlineMode.SOLID, Color.ORANGE),
                new RotateImage(new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.ORANGE),
                    35)),
            300, 200))
        && t.checkExpect(
            this.b6.draw(w1), w1
                .placeImageXY(
                    new BesideImage(new EllipseImage(20, 8, OutlineMode.SOLID, Color.GREEN),
                        new RotateImage(
                            new EquilateralTriangleImage(8, OutlineMode.SOLID, Color.GREEN), 35)),
                    100, 200));
  }

  // test validPos on integers less than, greater than, and within a bound.
  boolean testValidPos(Tester t) {
    return t.checkExpect(p1.validPos(-3, 600), 597) && t.checkExpect(p1.validPos(605, 600), 5)
        && t.checkExpect(p1.validPos(300, 600), 300) && t.checkExpect(p1.validPos(600, 600), 600)
        && t.checkExpect(p1.validPos(0, 600), 0) && t.checkExpect(b1.validPos(-3, 600), 597)
        && t.checkExpect(b1.validPos(605, 600), 5) && t.checkExpect(b1.validPos(300, 600), 300)
        && t.checkExpect(b1.validPos(600, 600), 600) && t.checkExpect(b1.validPos(0, 600), 0);
  }

  // test widthCombination in AFish
  boolean testWidthCombination(Tester t) {
    return t.checkExpect(p1.widthCombination(b1), 22.5)
        && t.checkExpect(b1.widthCombination(b2), 20.0);
  }

  // test distance in AFish
  boolean testDistance(Tester t) {
    return t.checkInexact(p1.distance(b6), 200.0, 0.001)
        && t.checkInexact(b6.distance(b7), 223.6, 0.001);
  }

  // test biggerThan in AFish
  boolean testBiggerThan(Tester t) {
    return t.checkExpect(p1.biggerThan(b1), true) && t.checkExpect(p1.biggerThan(b4), false)
        && t.checkExpect(b5.biggerThan(b2), true) && t.checkExpect(b1.biggerThan(b4), false);
  }

  // test move on a Fish, and on a PlayerFish (in all 4 directions)
  boolean testMove(Tester t) {
    return t.checkExpect(b6.move(), b8) && t.checkExpect(b7.move(), b9)
        && t.checkExpect(p1.move(0, 10), p1Down) && t.checkExpect(p1.move(0, -10), p1Up)
        && t.checkExpect(p1.move(-10, 0), p1Left) && t.checkExpect(p1.move(10, 0), p1Right);
  }

  // test grow on a playerfish given a list of fishes(1 test collides with a
  // smaller, 1 test does not collide)
  boolean testGrow(Tester t) {
    return t.checkExpect(
        p1.grow(new ConsList<Fish>(new Fish(25, 6, Color.GREEN, 299, 199, -1), mt)), p1Grow)
        && t.checkExpect(p1.grow(listfish3), p1) && t.checkExpect(p1.grow(mt), p1);
  }

  // ILIST TESTS (test mt list and conslists) & Predicates, Functions, BiFunctions

  // test filter with predicates BiggerThan, NotCollided, and SmallerThan
  boolean testBiggerThanPred(Tester t) {
    return t.checkExpect(new BiggerThan(15).test(this.b1), false)
        && t.checkExpect(new BiggerThan(8).test(this.b1), false)
        && t.checkExpect(new BiggerThan(6).test(this.b1), true);
  }

  boolean testNotCollidedPred(Tester t) {
    return t.checkExpect(new NotCollided(this.p1).test(this.b6), true)
        && t.checkExpect(new NotCollided(this.p1).test(this.b7), true) && t.checkExpect(
            new NotCollided(this.p1).test(new Fish(20, 8, Color.ORANGE, 298, 198, 1)), false);
  }

  boolean testSmallerThanPred(Tester t) {
    return t.checkExpect(new SmallerThan(15).test(b1), true)
        && t.checkExpect(new SmallerThan(8).test(b1), false)
        && t.checkExpect(new SmallerThan(6).test(b1), false);
  }

  // test filter in ILIST
  boolean testFilter(Tester t) {
    return t.checkExpect(listfish.filter(new SmallerThan(9)),
        new ConsList<Fish>(b3, new ConsList<Fish>(b2, new ConsList<Fish>(b1, new MtList<Fish>()))))
        && t.checkExpect(mt.filter(new NotCollided(this.p1)), this.mt)
        && t.checkExpect(listfish.filter(new BiggerThan(9)),
            new ConsList<Fish>(b5, new ConsList<Fish>(b4, new MtList<Fish>())));
  }

  // test movefish Function
  boolean testMoveFish(Tester t) {
    return t.checkExpect(new MoveFish().apply(this.b6), this.b8)
        && t.checkExpect(new MoveFish().apply(this.b7), this.b9);
  }

  // test map in ILIST
  boolean testMap(Tester t) {
    return t.checkExpect(new ConsList<Fish>(b6, new ConsList<Fish>(b7, mt)).map(new MoveFish()),
        new ConsList<Fish>(b8, new ConsList<Fish>(b9, mt)))
        && t.checkExpect(mt.map(new MoveFish()), mt);
  }

  // test drawfish bifunction
  boolean testDrawFish(Tester t) {
    return t.checkExpect(new DrawFish().apply(b6, w1),
        w1.placeImageXY(
            new BesideImage(new EllipseImage(20, 8, OutlineMode.SOLID, Color.GREEN),
                new RotateImage(new EquilateralTriangleImage(8, OutlineMode.SOLID, Color.GREEN),
                    35)),
            100, 200))
        && t.checkExpect(
            new DrawFish().apply(b8, w1), w1
                .placeImageXY(
                    new BesideImage(new EllipseImage(20, 8, OutlineMode.SOLID, Color.GREEN),
                        new RotateImage(
                            new EquilateralTriangleImage(8, OutlineMode.SOLID, Color.GREEN), 35)),
                    101, 200));
  }

  // test allsmaller bifunct
  boolean testAllSmaller(Tester t) {
    return t.checkExpect(new AllSmaller(this.p1).apply(b1, false), false)
        && t.checkExpect(new AllSmaller(this.p1).apply(b1, true), true)
        && t.checkExpect(new AllSmaller(this.p1).apply(b4, true), false)
        && t.checkExpect(new AllSmaller(this.p1).apply(b4, false), false);
  }

  // test collisioncheck bifunct
  boolean testCollisionCheck(Tester t) {
    return t.checkExpect(new CollisionCheck(this.p1).apply(b6, true), true) && t.checkExpect(
        new CollisionCheck(this.p1).apply(new Fish(25, 10, Color.GREEN, 299, 199, -1), true), true)
        && t.checkExpect(
            new CollisionCheck(this.p1).apply(new Fish(25, 10, Color.GREEN, 299, 199, -1), false),
            true)
        && t.checkExpect(new CollisionCheck(this.p1).apply(b6, false), false);
  }

  // test foldr in ILIST
  boolean testFoldR(Tester t) {
    return t.checkExpect(listfish.foldr(new AllSmaller(this.p1), true), false) && t
        .checkExpect(listfish.foldr(new AllSmaller(new PlayerFish(25, 30, 300, 200)), true), true)
        && t.checkExpect(mt.foldr(new AllSmaller(this.p1), true), true);
  }

  // FISHWORLD TESTS

  // test makeScene (end scene tested in endworld)
  boolean testMakeScene(Tester t) {
    return t.checkExpect(world2.makeScene(), this.p1.draw(this.listfish4.foldr(new DrawFish(), w1)))
        && t.checkExpect(world3.makeScene(),
            this.p1.draw(this.listfish5.foldr(new DrawFish(), w1)));
  }

  // test onKeyEvent
  boolean testOnKey(Tester t) {
    return t.checkExpect(world2.onKeyEvent("up"), new FishWorld(p1Up, this.listfish4))
        && t.checkExpect(world2.onKeyEvent("down"), new FishWorld(p1Down, this.listfish4))
        && t.checkExpect(world2.onKeyEvent("right"), new FishWorld(p1Right, this.listfish4))
        && t.checkExpect(world2.onKeyEvent("left"), new FishWorld(p1Left, this.listfish4))
        && t.checkExpect(world2.onKeyEvent("f"), world2);
  }

  // test moveFishes
  boolean testMoveFishes(Tester t) {
    return t.checkExpect(world2.moveFishes(), new FishWorld(p1, this.listfish5))
        && t.checkExpect(new FishWorld(this.p1, mt).moveFishes(), new FishWorld(p1, mt));
  }

  // test removeSmallerCollided()
  boolean testRemoveSmallerCollided(Tester t) {
    return t.checkExpect(world2.removeSmallerCollided(), world2)
        && t.checkExpect(world4.removeSmallerCollided(), world2)
        && t.checkExpect(new FishWorld(this.p1, mt).removeSmallerCollided(), new FishWorld(p1, mt));
  }

  // test growPlayer()
  boolean testGrowPlayer(Tester t) {
    return t.checkExpect(world3.growPlayer(), world3) && t.checkExpect(world4.growPlayer(), world5)
        && t.checkExpect(new FishWorld(this.p1, mt).growPlayer(), new FishWorld(p1, mt));
  }

  // test worldEnds()
  boolean testWorldEnds(Tester t) {
    Fish smallf = new Fish(25, 6, Color.GREEN, 200, 199);
    IList<Fish> smallList = new ConsList<Fish>(smallf, mt);
    FishWorld smallW = new FishWorld(p1, smallList);
    Fish largef = new Fish(30, 20, Color.GREEN, 300, 200, 1);
    IList<Fish> largeCollision = new ConsList<Fish>(largef, mt);
    FishWorld largeCol = new FishWorld(p1, largeCollision);
    Fish bigNoCol = new Fish(25, 15, Color.GREEN, 100, 100);
    IList<Fish> bigListNoCol = new ConsList<Fish>(bigNoCol, mt);
    FishWorld bigNoCollisions = new FishWorld(p1, bigListNoCol);
    FishWorld mtworld = new FishWorld(p1, mt);

    return t.checkExpect(smallW.worldEnds(), new WorldEnd(true, smallW.showWin(true)))
        && t.checkExpect(largeCol.worldEnds(), new WorldEnd(true, largeCol.showWin(false)))
        && t.checkExpect(bigNoCollisions.worldEnds(),
            new WorldEnd(false, bigNoCollisions.showWin(false)))
        && t.checkExpect(mtworld.worldEnds(), new WorldEnd(true, smallW.showWin(true)));
  }

  // test showWin
  boolean testShowWin(Tester t) {
    return t.checkExpect(world2.showWin(true),
        w1.placeImageXY(new TextImage("You Won!", Color.GREEN), FishWorld.GAME_LENGTH / 2,
            FishWorld.GAME_HEIGHT / 2))
        && t.checkExpect(world2.showWin(false),
            w1.placeImageXY(new TextImage("You Lost!", Color.RED), FishWorld.GAME_LENGTH / 2,
                FishWorld.GAME_HEIGHT / 2));
  }

}
