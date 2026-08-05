# Project 3 Prep

**For tessellating hexagons, one of the hardest parts is figuring out where to place each hexagon/how to easily place hexagons on screen in an algorithmic way.
After looking at your own implementation, consider the implementation provided near the end of the lab.
How did your implementation differ from the given one? What lessons can be learned from it?**

Answer: no Hexagon class, same when draw hex col 

-----

**Can you think of an analogy between the process of tessellating hexagons and randomly generating a world using rooms and hallways?
What is the hexagon and what is the tesselation on the Project 3 side?**

Answer: In Lab 12, a single hexagon is the basic building block, and tessellation is the process of arranging many hexagons into a complete world. 
Similarly, in Project 3, a room is the basic building block, and world generation consists of placing many randomly sized and positioned rooms, 
then connecting them with hallways to form a complete, connected world.

-----
**If you were to start working on world generation, what kind of method would you think of writing first? 
Think back to the lab and the process used to eventually get to tessellating hexagons.**

Answer: make floor rectangle tile -> make multi floor 

-----
**What distinguishes a hallway from a room? How are they similar?**

Answer: Rooms and hallways are both walkable areas made up of floor tiles surrounded by walls. Their main difference lies in their shape and purpose. 
Rooms are typically larger, open spaces where the player can move freely, while hallways are narrow passages used to connect rooms together.
