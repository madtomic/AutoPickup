package me.MnMaxon.AutoPickup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class MainListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent e) {
		Player p = e.getPlayer();
		Main.guess.remove(p.getName());
		List<Material> guessDrops = new ArrayList<Material>();
		for (ItemStack guessMat : e.getBlock().getDrops(p.getItemInHand()))
			guessDrops.add(guessMat.getType());
		Main.guess.put(p.getName(), guessDrops);
		boolean isNatural = Natural.isNatural(e.getBlock());
		Natural.naturalize(e.getBlock());
		if (p.hasPermission(Permissions.INFINITE_PICK) && p.getItemInHand().getType().name().contains("PICK"))
			p.getItemInHand().setDurability((short) -1);

		if ((p.hasPermission(Permissions.AUTO_SMELT)
				&& (e.getBlock().getType().equals(Material.IRON_ORE) || e.getBlock().getType()
						.equals(Material.GOLD_ORE)) || Main.superFortune)

				&& !p.getGameMode().equals(GameMode.CREATIVE)
				&& p.getItemInHand().containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)
				&& WhiteList.getBlocks(p.getItemInHand()).contains(e.getBlock().getType()) && isNatural) {
			int i = p.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
			if (p.getItemInHand().getDurability() - 1 <= 0)
				p.getItemInHand().setType(Material.AIR);
			else
				p.getItemInHand().setDurability((short) (p.getItemInHand().getDurability() - 1));
			for (ItemStack is : e.getBlock().getDrops(e.getPlayer().getItemInHand())) {
				if (is.getAmount() == 1) {
					is.setAmount(new Random().nextInt(i));
					Location loc = e.getBlock().getLocation().add(.5, .5, .5);
					MetaLists.who.add(loc.getWorld().dropItemNaturally(loc, is), p);
				}
			}
		}
		if (Check.hasAutoXP(p)) {
			p.giveExp(e.getExpToDrop());
			e.setExpToDrop(0);
		}
		if (e.getBlock().getState() instanceof InventoryHolder) {
			InventoryHolder ih = (InventoryHolder) e.getBlock().getState();
			ItemStack[] conts = ih.getInventory().getContents().clone();
			ih.getInventory().clear();
			e.setCancelled(true);
			e.getBlock().breakNaturally();
			for (ItemStack is : conts)
				if (is != null) {
					NoPickup.add(is);
					e.getBlock().getLocation().getWorld()
							.dropItemNaturally(e.getBlock().getLocation().add(.5, .5, .1), is);
				}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onItemSpawn(ItemSpawnEvent e) {
		Location orignalLoc = e.getLocation();
		if (!NoPickup.canPickup(e.getEntity().getItemStack())) {
			e.getEntity().setItemStack(NoPickup.remove(e.getEntity().getItemStack()));
			return;
		}

		Player p = null;
		if (MetaLists.who.contains(e.getEntity())
				&& (Bukkit.getPlayer((String) MetaLists.who.get(e.getEntity()))) != null)
			p = Bukkit.getPlayer((String) MetaLists.who.get(e.getEntity()));
		if (p == null) {
			p = getNearby(e.getEntity(), .5);
			if (p == null) {
				p = getNearby(e.getEntity(), 1);
				if (p == null) {
					p = getNearby(e.getEntity(), 2);
					if (p == null) {
						p = getNearby(e.getEntity(), 3);
						if (p == null) {
							p = getNearby(e.getEntity(), 4);
							if (p == null) {
								p = getNearby(e.getEntity(), 5);
								if (p == null) {
									p = getNearby(e.getEntity(), 6);
									if (p == null) {
										return;
									}
								}
							}
						}
					}
				}
			}

			if (!Main.guess.containsKey(p.getName())
					|| !Main.guess.get(p.getName()).contains(e.getEntity().getItemStack().getType())) {
				for (Entity ent : e.getEntity().getNearbyEntities(6, 6, 6))
					if (ent instanceof Player
							&& ent.isValid()
							&& Main.guess.containsKey(((Player) ent).getName())
							&& Main.guess.get(((Player) ent).getName())
									.contains(e.getEntity().getItemStack().getType()))
						p = (Player) ent;
			}
		}
		Main.guess.remove(p.getName());

		ArrayList<ItemStack> finalItems = new ArrayList<ItemStack>();
		finalItems.add(e.getEntity().getItemStack());

		if (p.hasPermission(Permissions.AUTO_SMELT)) {
			for (ItemStack is : finalItems)
				if (Main.blocksToSmelt.containsKey(is.getType()))
					is.setType(Main.blocksToSmelt.get(is.getType()).getNewType());
		}

		if (!Check.hasAutoPickup(p)) {
			e.getEntity().remove();
			for (ItemStack is : finalItems) {
				NoPickup.add(is);
				orignalLoc.getWorld().dropItem(orignalLoc, is);
			}
			return;
		}

		if (p.hasPermission(Permissions.AUTO_BLOCK)) {
			ItemStack[] newInvCont = Main.convertToBlocks(p.getInventory().getContents());
			if (!p.getInventory().getContents().equals(newInvCont)) {
				p.getInventory().setContents(newInvCont);
				p.updateInventory();
			}
		}

		finalItems = Main.addToInventory(p, finalItems);
		if (!finalItems.isEmpty()) {
			if (!FullInventory.hasCooldown(p)) {
				Messages.send(p, Messages.FULL_INVENTORY);
				FullInventory.addCooldown(p);
			}
			if (Main.fullDrop)
				for (ItemStack is : finalItems) {
					NoPickup.add(is);
					orignalLoc.getWorld().dropItemNaturally(orignalLoc, is);
				}
		}
		e.getEntity().remove();
	}

	private Player getNearby(Item item, double range) {
		for (Entity e : item.getNearbyEntities(range, range, range))
			if (e instanceof Player)
				return (Player) e;
		return null;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent e) {
		Natural.deNaturalize(e.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent e) {
		Player killer = e.getEntity().getKiller();
		if (killer != null && !Check.hasAutoPickupMob(killer)) {
			for (ItemStack is : e.getDrops())
				NoPickup.add(is);
			return;
		}
		ArrayList<ItemStack> drops = new ArrayList<ItemStack>(e.getDrops());
		e.getDrops().clear();
		if (e.getEntity() instanceof Player)
			for (ItemStack is : drops) {
				is = (NoPickup.add(is));
				e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), is);
			}
		else if (killer != null && killer.isValid()) {
			for (ItemStack is : drops)
				killer.getWorld().dropItemNaturally(killer.getLocation(), is);
		}
		if (killer != null && !(e.getEntity() instanceof Player) && Check.hasAutoXP(killer)) {
			killer.giveExp(e.getDroppedExp());
			e.setDroppedExp(0);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemDrop(PlayerDropItemEvent e) {
		if (!e.isCancelled())
			e.getItemDrop().setItemStack(NoPickup.add(e.getItemDrop().getItemStack()));
	}
}