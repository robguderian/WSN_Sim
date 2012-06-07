package sim_core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.JFileChooser;

import Util.Utility;

public class Configuration {

	public static boolean verbose = true;

	private static ArrayList <NodeConfig> nodeConfig = new ArrayList<NodeConfig>();
	
	
	public static void configurationReader(String filename)
	{
		if (nodeConfig.size() == 0)
			nodeConfig.add(new NodeConfig(""));
		
		//String currentDir = new File(".").getAbsolutePath();
		//System.out.println(currentDir);
		System.out.println("Reading in config files.");
		try {
			FileReader theFile = new FileReader( "defaults.txt" );
			readConfig(theFile);
			System.out.println("Read defaults");
		} catch (FileNotFoundException e) {
			System.out.println("Defaults config not read");
		}
		try {
			FileReader theFile = new FileReader( filename );
			readConfig(theFile);
			System.out.println("Read "+ filename);
		} catch (FileNotFoundException e) {
			System.out.println("No config read");
		}
		if (configExists("verbose"))
			verbose = getBooleanConfig("verbose");
	}
	
	public static void additionalConfig(String filename)
	{
		System.out.println("Reading in additional config file");
		try {
			FileReader theFile = new FileReader( filename );
			readConfig(theFile);
			System.out.println("Read "+ filename);
		} catch (FileNotFoundException e) {
			System.out.println("No config read");
		}
		if (configExists("verbose"))
			verbose = getBooleanConfig("verbose");
	}
	
	public static String[] getNodeConfigNames()
	{
		String [] nodeNames = new String[ nodeConfig.size()-1];
		
		for (int i = 0; i < nodeConfig.size() -1 ; i++)
		{
			nodeNames[i] = nodeConfig.get(i+1).getName();
		}
		return nodeNames;
	}
	public static void configurationReaderPretty()
	{
	    JFileChooser pickFile = new JFileChooser(); // Dialog box to pick
	    if(pickFile.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
	    {
	    	
	    	try {
				FileReader theFile = new FileReader( pickFile.getSelectedFile() );
				readConfig(theFile);
			} catch (FileNotFoundException e) {
				System.out.println("No config read");
			}
	    	
	    }
	}
	
	private static void readConfig(FileReader file)
	{
		// configuration items are in name value pairs.
		String[] parts;
		
		BufferedReader in = new BufferedReader( file );
		
		String line;
		NodeConfig curr = nodeConfig.get( 0 );
		
		try {
			line = in.readLine();
				
			while (line != null)
			{
				parts = line.split("\\s+");
				
				if (parts.length >= 2 && !line.startsWith("#") && !line.startsWith("["))
				{
					if (curr.configExists(parts[0]))
						curr.updateConfig(parts[0], parts[1]);
					else
						curr.addConfig(new ConfigurationItem(parts[0], parts[1]));
				}
				else if (line.startsWith("["))
				{
					curr = new NodeConfig(parts[1].substring(0, parts[1].length()-1)); // making some assumptions here. deal with it.
					nodeConfig.add(curr);
					
					
					// do a deep copy of the defaults
					for (String key : nodeConfig.get(0).getKeys())
					{
						curr.addConfig(key, nodeConfig.get(0).getConfig(key));
					}
						
					
				}
				line = in.readLine();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void updateAllConfigs(String config, String value)
	{
		for (NodeConfig n : nodeConfig)
		{
			if (n.configExists(config))
			{
				n.updateConfig(config, value);
			}
			else
				n.addConfig(config, value);
		}
	}
	
	public static boolean configExists(String which, String config)
	{
		for (NodeConfig n : nodeConfig)
			if (n.getName().equals(which))
				return n.configExists(config);
		return false;
	}
	
	public  static String getConfig(String which, String config)
	{
		for (NodeConfig n : nodeConfig)
			if (n.getName().equals(which))
				return n.getConfig(config);
		return null;
	}
	public static  int getIntConfig(String which, String config)
	{
		for (NodeConfig n : nodeConfig)
			if (n.getName().equals(which))
				return n.getIntConfig(config);
		
		return Integer.MIN_VALUE;
	}
	public  static double getDoubleConfig(String which, String config)
	{
		for (NodeConfig n : nodeConfig)
			if (n.getName().equals(which))
				return n.getDoubleConfig(config);
		return Double.MIN_VALUE;
	}
	
	public static boolean getBooleanConfig(String which, String config)
	{
		for (NodeConfig n : nodeConfig)
			if (n.getName().equals(which))
				return n.getBooleanConfig(config);
		return false;

	}
	
	/*
	 * wrapper methods to allow for the defaults to be easily grabbed.
	 */
	
	public static boolean configExists(String config)
	{
		return configExists("", config);
	}
	
	public static  String getConfig(String config)
	{
		return getConfig("", config);
	}
	public static  int getIntConfig( String config)
	{
		return getIntConfig("", config);
	}
	public static  double getDoubleConfig(String config)
	{
		return getDoubleConfig("", config);
	}
		
	public static boolean getBooleanConfig(String config)
	{
		return getBooleanConfig("", config);
	}
	
}

class NodeConfig  {
	//private  ArrayList <ConfigurationItem> config = new ArrayList<ConfigurationItem>();
	Hashtable<String, ConfigurationItem> config     = new Hashtable<String, ConfigurationItem>();
	private String name;
	
	public NodeConfig(String name)
	{
		this.name = name;
	}
	
	

	public String getName()
	{
		return name;
	}
	
	public void addConfig(ConfigurationItem i)
	{
		config.put(i.name, i);
	}
	
	public void addConfig(String name, String value)
	{
		config.put(name,  new ConfigurationItem(name,value));
	}
	
	public  boolean configExists(String name)
	{	
		return config.containsKey(name);
	}
	
	public  String getConfig(String name)
	{
		return config.get(name).getValue();
	}
	public  int getIntConfig(String name)
	{
		return config.get(name).getIntValue();
	}
	public  double getDoubleConfig(String name)
	{
		return config.get(name).getDoubleValue();
	}
	
	public boolean getBooleanConfig(String name)
	{
		return config.get(name).getBooleanValue();
	}
	
	public void updateConfig(String name, String value) 
	{
		if (config.containsKey(name))
		{
			config.remove(name);
			addConfig(name,value);
		}
		else
		{
			ConfigurationItem old = config.get(name);
			old.updateData(value);
		}
					
	}
		
	public Set<String> getKeys()
	{
		return config.keySet();
		
	}
}

class ConfigurationItem
{
	public String name;
	public String value;
	
	public ConfigurationItem(String name, String value)
	{
		this.name = name;
		this.value = value;
	}
	
	public void updateData(String newValue) 
	{
		this.value = newValue;
		
	}

	public String getName()
	{
		return name;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public int getIntValue()
	{
		int ret = Integer.MIN_VALUE;
		
		if (Utility.isInteger(value)){
			ret = Integer.parseInt(value);
		}
		return ret;
	}
	
	public double getDoubleValue()
	{
		double ret = Double.MIN_VALUE;
		
		if (Utility.isDouble(value)){
			ret = Double.parseDouble(value);
		}
		return ret;
	}
	
	public boolean getBooleanValue()
	{
		
		if (value.equalsIgnoreCase("true"))
			return true;
		return false;
	}
}