package me.MnMaxon.AutoPickup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Guess {
	public static long cooldown = 100;
	public long time;
	public ArrayList<Material> materials;

	public Guess(ArrayList<Material> materials) {
		time = new Date().getTime() + cooldown;
		this.materials = materials;
	}

	public static boolean fits(Player p, Material m) {
		if (MetaLists.guess.contains(p)) {
			Guess guess = (Guess) MetaLists.guess.get(p);
			if (guess.time < new Date().getTime() && guess.materials.contains(m))
				return true;
		}
		return false;
	}

	public static void add(Player p, ArrayList<Material> materials) {
		MetaLists.guess.add(p, new Guess(materials));
	}

	public static void add(Player p, Material m) {
		ArrayList<Material> materials = new ArrayList<Material>();
		materials.add(m);
		add(p, materials);
	}

	public static void add(Player p, Collection<ItemStack> drops) {
		ArrayList<Material> materials = new ArrayList<Material>();
		for (ItemStack is : drops)
			materials.add(is.getType());
		add(p, materials);
	}
}
