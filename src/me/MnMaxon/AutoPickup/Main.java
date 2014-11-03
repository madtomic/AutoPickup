package me.MnMaxon.AutoPickup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
	public static String dataFolder;

	public static Main plugin;

	public static SuperYaml MainConfig;
	public static SuperYaml MessageConfig;
	public static SuperYaml PermissionConfig;
	public static SuperYaml WhiteListConfig;
	public static SuperYaml DataConfig;

	public static Map<Material, AutoBlockInfo> blocksToAuto = new HashMap<Material, AutoBlockInfo>();
	public static Map<Material, AutoBlockInfo> blocksToSmelt = new HashMap<Material, AutoBlockInfo>();
	public static Map<String, List<Material>> guess = new HashMap<String, List<Material>>();

	public static boolean fullDrop = false;
	public static boolean superFortune = false;

	@Override
	public void onEnable() {
		plugin = this;
		dataFolder = this.getDataFolder().getAbsolutePath();
		reloadConfigs();
		setUpSmeltBlocks();
		setUpAutoBlocks();
		getServer().getPluginManager().registerEvents(new MainListener(), this);
	}

	@Override
	public void onDisable() {
		Natural.save();
	}

	private void setUpAutoBlocks() {
		blocksToAuto.put(Material.DIAMOND, new AutoBlockInfo(9, Material.DIAMOND_BLOCK, 1));
		blocksToAuto.put(Material.EMERALD, new AutoBlockInfo(9, Material.EMERALD_BLOCK, 1));
		blocksToAuto.put(Material.GOLD_INGOT, new AutoBlockInfo(9, Material.GOLD_BLOCK, 1));
		blocksToAuto.put(Material.GOLD_NUGGET, new AutoBlockInfo(9, Material.GOLD_INGOT, 1));
		blocksToAuto.put(Material.IRON_INGOT, new AutoBlockInfo(9, Material.IRON_BLOCK, 1));
		blocksToAuto.put(Material.INK_SACK, new AutoBlockInfo(9, Material.LAPIS_BLOCK, 1));
		blocksToAuto.put(Material.QUARTZ, new AutoBlockInfo(4, Material.QUARTZ_BLOCK, 1));
		blocksToAuto.put(Material.REDSTONE, new AutoBlockInfo(9, Material.REDSTONE_BLOCK, 1));
		blocksToAuto.put(Material.COAL, new AutoBlockInfo(9, Material.COAL_BLOCK, 1));
	}

	private void setUpSmeltBlocks() {
		blocksToSmelt.put(Material.DIAMOND_ORE, new AutoBlockInfo(9, Material.DIAMOND, 1));
		blocksToSmelt.put(Material.EMERALD_ORE, new AutoBlockInfo(9, Material.EMERALD, 1));
		blocksToSmelt.put(Material.GOLD_ORE, new AutoBlockInfo(9, Material.GOLD_INGOT, 1));
		blocksToSmelt.put(Material.IRON_ORE, new AutoBlockInfo(9, Material.IRON_INGOT, 1));
	}

	public static void reloadConfigs() {
		MainConfig = new SuperYaml(dataFolder + "/Config.yml");

		WhiteList.setUp();
		Messages.setUp();
		Natural.setUp();
		Permissions.setUp();
		if (MainConfig.get("AutoBlock") != null)
			MainConfig.set("AutoBlock", null);
		if (MainConfig.get("AutoSmelt") != null)
			MainConfig.set("AutoSmelt", null);

		fullDrop = (boolean) easyConfig("Drop On Full Inventory", false);
		superFortune = (boolean) easyConfig("Fortune All", false);

		MainConfig.save();
	}

	public static Object easyConfig(String path, Object value) {
		Object originalValue = MainConfig.get(path);
		if (originalValue == null || !originalValue.getClass().equals(value.getClass())) {
			MainConfig.set(path, value);
			return value;
		} else
			return MainConfig.get(path);
	}

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if (s instanceof Player) {
			Player p = (Player) s;
			if (cmd.getLabel().equalsIgnoreCase("AutoPickupToggle"))
				if (p.hasPermission(Permissions.TOGGLE)) {
					if (args.length == 0)
						toggle(p);
					else if (args.length == 1 && args[0].equalsIgnoreCase("on"))
						toggle(p, true);
					else if (args.length == 1 && args[0].equalsIgnoreCase("off"))
						toggle(p, false);
					else
						Messages.send(p, ChatColor.RED + "Use like: /apt [on/off]");
				} else
					Messages.send(p, ChatColor.RED + "You do not have permission for this command!");
			else if (cmd.getLabel().equalsIgnoreCase("AutoBlock"))
				if (p.hasPermission(Permissions.COMMANDS_BLOCK)) {
					ItemStack[] newInvCont = Main.convertToBlocks(p.getInventory().getContents());
					if (!p.getInventory().getContents().equals(newInvCont)) {
						p.getInventory().setContents(newInvCont);
						p.updateInventory();
						Messages.send(p, Messages.COMMAND_BLOCK);
					} else
						Messages.send(p, Messages.COMMAND_BLOCK_FAIL);
				} else
					Messages.send(p, ChatColor.RED + "You do not have permission for this command!");
			else if (cmd.getLabel().equalsIgnoreCase("AutoSmelt"))
				if (p.hasPermission(Permissions.COMMANDS_SMELT)) {
					ItemStack[] newInvCont = Main.convertToBlocks(p.getInventory().getContents());
					for (int i = 0; i < newInvCont.length; i++)
						if (newInvCont[i] != null && Main.blocksToSmelt.containsKey(newInvCont[i].getType()))
							newInvCont[i].setType(Main.blocksToSmelt.get(newInvCont[i].getType()).getNewType());

					if (!p.getInventory().getContents().equals(newInvCont)) {
						p.getInventory().setContents(newInvCont);
						p.updateInventory();
						newInvCont = Main.convertToBlocks(p.getInventory().getContents());
						for (int i = 0; i < newInvCont.length; i++)
							if (newInvCont[i] != null && Main.blocksToSmelt.containsKey(newInvCont[i].getType()))
								newInvCont[i].setType(Main.blocksToSmelt.get(newInvCont[i].getType()).getNewType());
						p.getInventory().setContents(newInvCont);
						p.updateInventory();
						Messages.send(p, Messages.COMMAND_SMELT);
					} else
						Messages.send(p, Messages.COMMAND_SMELT_FAIL);
				} else
					Messages.send(p, ChatColor.RED + "You do not have permission for this command!");
		}
		if (cmd.getLabel().equalsIgnoreCase("AutoPickup"))
			if (!(s instanceof Player) || ((Player) s).hasPermission(Permissions.RELOAD)) {
				reloadConfigs();
				s.sendMessage(ChatColor.GREEN + "Reload successful!");
			} else
				s.sendMessage(ChatColor.RED + "You do not have permission for this command!");
		return false;
	}

	public static ArrayList<ItemStack> addToInventory(Player p, ArrayList<ItemStack> finalItems) {
		ArrayList<ItemStack> remaining = new ArrayList<ItemStack>();
		for (ItemStack is : finalItems) {
			HashMap<Integer, ItemStack> leftOver = p.getInventory().addItem(is);
			for (ItemStack left : leftOver.values())
				remaining.add(left);
		}
		return remaining;
	}

	public static ItemStack[] convertToBlocks(ItemStack[] startingItems) {
		int inventorySize = startingItems.length;
		ItemStack[] finalItems = Bukkit.createInventory(null, InventoryType.PLAYER).getContents();
		Map<Material, ArrayList<Integer>> toConvert = new HashMap<Material, ArrayList<Integer>>();
		for (int i = 0; i < inventorySize; i++) {
			ItemStack is = startingItems[i];
			if (is == null)
				;// DO NOTHING
			else if (!Main.blocksToAuto.containsKey(is.getType())) {
				finalItems[i] = is;
			} else {
				ArrayList<Integer> intList = new ArrayList<Integer>();
				if (toConvert.containsKey(is.getType())) {
					intList = toConvert.get(is.getType());
					toConvert.remove(is.getType());
				}
				intList.add(i);
				toConvert.put(is.getType(), intList);
			}
		}
		for (Entry<Material, ArrayList<Integer>> entry : toConvert.entrySet()) {
			AutoBlockInfo info = Main.blocksToAuto.get(entry.getKey());
			Material type = info.getNewType();
			int requiredAmount = info.getRequiredAmount();
			int createdAmount = info.getCreatedAmount();

			int leftOver = 0;
			for (int i : entry.getValue())
				leftOver += startingItems[i].getAmount();

			int created = 0;
			boolean changed = false;
			boolean good = false;
			while (!good) {
				if (leftOver >= requiredAmount) {
					changed = true;
					leftOver -= requiredAmount;
					created += createdAmount;
				} else
					good = true;
			}
			if (changed) {
				ArrayList<ItemStack> toAdd = new ArrayList<ItemStack>();
				ItemStack createdIS = new ItemStack(type);
				ItemStack excessIS = new ItemStack(entry.getKey());

				good = false;
				while (!good) {
					if (created > 64) {
						created -= 64;
						createdIS.setAmount(64);
					} else {
						createdIS.setAmount(created);
						good = true;
					}
					if (createdIS.getAmount() <= 0)
						good = true;
					else
						toAdd.add(createdIS);
				}

				good = false;
				while (!good) {
					if (leftOver > 64) {
						leftOver -= 64;
						excessIS.setAmount(64);
					} else {
						excessIS.setAmount(leftOver);
						good = true;
					}
					if (excessIS.getAmount() <= 0)
						good = true;
					else
						toAdd.add(excessIS);
				}

				Inventory tempInv = Bukkit.createInventory(null, InventoryType.PLAYER);
				tempInv.setContents(finalItems);
				for (ItemStack is : toAdd) {
					tempInv.addItem(is);
				}
				finalItems = tempInv.getContents();
			} else {
				for (int i : entry.getValue())
					finalItems[i] = startingItems[i];
			}
		}
		return finalItems;
	}

	public String getCurrentVersion() {
		return this.getDescription().getVersion();
	}

	public static void toggle(Player p, Boolean using) {
		if (using) {
			MetaLists.off.remove(p);
			Messages.send(p, Messages.PICKUP_ENABLE);
		} else {
			MetaLists.off.add(p);
			Messages.send(p, Messages.PICKUP_DISABLE);
		}
	}

	public static void toggle(Player p) {
		toggle(p, (MetaLists.off.contains(p)) ? true : false);
	}
}