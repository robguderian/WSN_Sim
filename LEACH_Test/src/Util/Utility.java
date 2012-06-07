package Util;

import umontreal.iro.lecuyer.rng.RandomStream;

public class Utility {

    public static boolean isInteger( String input )  
    {  
       try  
       {  
          Integer.parseInt( input );  
          return true;  
       }  
       catch( Exception e)  
       {  
          return false;  
       }  
    }  
    
    public static boolean isDouble( String input )  
    {  
       try  
       {  
          Double.parseDouble( input );  
          return true;  
       }  
       catch( Exception e)  
       {  
          return false;  
       }  
    }  
    
    public static double randU01(RandomStream r)
    {
    	return r.nextDouble();
    }
    
    

}
