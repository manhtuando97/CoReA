import java.io.*;
import java.util.*;

import util.CoReA;

public class main{


    public static void main(String[] args) throws IOException{

        String name = args[0];
        int max_node_id = Integer.parseInt(args[1]);
        String tie = args[2];
        int degeneracy = Integer.parseInt(args[3]);
        String scheme = args[4];
        String rank = args[5];
        int budget = Integer.parseInt(args[6]);

        CoReA h_graph = new CoReA(name, max_node_id, tie, scheme, degeneracy, rank, budget);
       
    }

}
