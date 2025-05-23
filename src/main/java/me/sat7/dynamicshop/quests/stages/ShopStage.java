package me.sat7.dynamicshop.quests.stages;

import fr.skytasul.quests.api.QuestsPlugin;
import fr.skytasul.quests.api.comparison.ItemComparisonMap;
import fr.skytasul.quests.api.editors.TextEditor;
import fr.skytasul.quests.api.editors.parsers.NumberParser;
import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.localization.Lang;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.players.PlayerAccount;
import fr.skytasul.quests.api.players.PlayersManager;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageController;
import fr.skytasul.quests.api.stages.StageDescriptionPlaceholdersContext;
import fr.skytasul.quests.api.stages.creation.StageCreation;
import fr.skytasul.quests.api.stages.creation.StageCreationContext;
import fr.skytasul.quests.api.stages.creation.StageGuiLine;
import fr.skytasul.quests.api.utils.Utils;
import fr.skytasul.quests.api.utils.XMaterial;
import fr.skytasul.quests.gui.items.ItemComparisonGUI;
import me.sat7.dynamicshop.events.ShopBuySellEvent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.OptionalInt;

public class ShopStage extends AbstractStage implements Listener {

    String shopName;
    ItemStack itemStack;
    ItemComparisonMap comparisons;

    int amount;

    int buy;


    public ShopStage(@NotNull StageController controller, @Nullable String shopName, @Nullable ItemStack itemStack, int amount, ItemComparisonMap comparisons, int buy) {
        super(controller);
        this.itemStack = itemStack;
        this.amount = amount;
        this.comparisons = comparisons;
        this.shopName = shopName;
        this.buy = buy;
    }

    @EventHandler
    public void onBuy(ShopBuySellEvent event) {
        Player p = event.getPlayer();
        if ((((buy == 0 && event.isBuy()) || (buy == 1 && !event.isBuy())) || buy == 2) && hasStarted(p) && canUpdate(p, true)) {
            if (shopName == null || shopName.equals(event.getShopName())) {
                if (itemStack == null || comparisons.isSimilar(itemStack, event.getItemStack())) {
                    int amount = event.getItemStack().getAmount();
                    add(p, amount);
                }
            }
        }
    }

    @Override
    public @NotNull String getDefaultDescription(@NotNull StageDescriptionPlaceholdersContext context) {
        String str = "";
        if (shopName != null) {
            str = shopName + "에서 ";
        }
        if (itemStack != null) {
            str += ItemUtils.getName(itemStack, true);
        } else {
            str += "아이템";
        }
        return str + "§e " + (buy==0? "구매하기": buy==1?"판매하기":"거래하기") + "(" + getPlayerAmountOptional(context.getPlayerAccount()).orElse(0) + "/" + amount + ")";
    }


    protected void add(@NotNull Player p, int amount) {
        PlayerAccount acc = PlayersManager.getPlayerAccount(p);
        if (hasStarted(p) && canUpdate(p)) {
            OptionalInt playerAmount = getPlayerAmountOptional(acc);
            if (!playerAmount.isPresent()) {
                QuestsPlugin.getPlugin().getLoggerExpanded().warning(p.getName() + " does not have object datas for stage " + this + ". This is a bug!");
            } else if (playerAmount.getAsInt()+amount >= this.amount) {
                finishStage(p);
            } else {
                updateObjective(p, "amount", playerAmount.getAsInt() + amount);
            }
        }
    }

    @Override
    public void initPlayerDatas(@NotNull PlayerAccount acc, @NotNull Map<@NotNull String, @Nullable Object> datas) {
        super.initPlayerDatas(acc, datas);
        datas.put("amount", 0);
    }

    @NotNull
    private OptionalInt getPlayerAmountOptional(@NotNull PlayerAccount acc) {
        Integer amount = getData(acc, "amount");
        return amount == null ? OptionalInt.empty() : OptionalInt.of(amount.intValue());
    }

    @Override
    protected void serialize(ConfigurationSection section) {
        if (itemStack != null) section.set("itemStack", itemStack.serialize());

        if (!comparisons.getNotDefault().isEmpty()) section.createSection("itemComparisons", comparisons.getNotDefault());
        if (shopName != null) section.set("shopName", shopName);
        section.set("amount", amount);
        section.set("buy", buy);
    }

    public static class Creator extends StageCreation<ShopStage> {
        String shopName;
        ItemStack itemStack;
        ItemComparisonMap comparisons = new ItemComparisonMap();
        int amount = 1;
        int buy = 0;

        public Creator(@NotNull StageCreationContext<ShopStage> context) {
            super(context);
        }

        @Override
        protected @NotNull ShopStage finishStage(@NotNull StageController branch) {
            return new ShopStage(branch, shopName, itemStack, amount, comparisons, buy);
        }

        @Override
        public void setupLine(@NotNull StageGuiLine line) {
            super.setupLine(line);

            line.setItem(5, ItemUtils.item(XMaterial.OAK_SIGN, "상점 이름"), event -> {
                new TextEditor<>(event.getPlayer(), event::reopen, x -> {
                    setShopName(x.toString());
                    event.reopen();
                }).passNullIntoEndConsumer().start();
            });
            line.setItem(6, ItemUtils.item(XMaterial.CHEST, Lang.editItem.toString()), event -> {
                QuestsPlugin.getPlugin().getGuiManager().getFactory().createItemSelection(is -> {
                    if (is != null)
                        setItem(is);
                    event.reopen();
                }, true).open(event.getPlayer());
            });
            line.setItem(7, ItemUtils.item(XMaterial.PRISMARINE_SHARD, Lang.stageItemsComparison.toString()), event -> {
                new ItemComparisonGUI(comparisons, () -> {
                    setComparisons(comparisons);
                    event.reopen();
                }).open(event.getPlayer());
            });

            line.setItem(8, ItemUtils.item(XMaterial.PAPER, "횟수 변경"), event -> {
                new TextEditor<>(event.getPlayer(), event::reopen, x -> {
                    setAmount(x);
                    event.reopen();
                }, NumberParser.INTEGER_PARSER_STRICT_POSITIVE).start();
            });
            line.setItem(9, getBuy(buy), event -> {
                setBuy(buy +1);
            });
        }

        public void setShopName(String shopName) {
            this.shopName = shopName;
            getLine().refreshItemLoreOptionValue(5, shopName);
        }
        public void setItem(ItemStack item) {
            this.itemStack = item;
            getLine().refreshItem(6,
                    item2 -> ItemUtils.lore(item2,
                            QuestOption.formatNullableValue(Utils.getStringFromItemStack(item, "§8", true))));
        }

        public void setComparisons(ItemComparisonMap comparisons) {
            this.comparisons = comparisons;
            getLine().refreshItem(7, item -> ItemUtils.lore(item,
                    QuestOption.formatNullableValue(
                            Lang.AmountComparisons.quickFormat("amount", this.comparisons.getEffective().size()))));
        }
        public void setAmount(int amount) {
            this.amount = amount;
            getLine().refreshItemName(8, Lang.Amount.quickFormat("amount", amount));
        }
        public void setBuy(int buy) {
            this.buy = (buy % 3);
            getLine().refreshItem(9, item -> setBuy(item, this.buy));
        }
        private ItemStack getBuy(int buy) {
            return ItemUtils.item(buy==0?XMaterial.LIME_DYE:buy==1?XMaterial.PINK_DYE:XMaterial.GRAY_DYE, buy==0?"§a구매하기":buy==1?"§c판매하기":"§e거래하기");
        }

        private ItemStack setBuy(ItemStack item, int buy) {
            item.setType(buy==0?Material.LIME_DYE:buy==1?Material.PINK_DYE:Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(buy==0?"§a구매하기":buy==1?"§c판매하기":"§e거래하기");
            item.setItemMeta(meta);
            return item;
        }

        @Override
        public void edit(@NotNull ShopStage stage) {
            super.edit(stage);
            if (stage.shopName != null) setShopName(stage.shopName);
            if (stage.itemStack != null) setItem(stage.itemStack);
            setComparisons(stage.comparisons.clone());
            setAmount(stage.amount);
            setBuy(stage.buy);
        }
    }
}
