package com.waldron.adventurerspack;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.nbt.CompoundTag;

public class PackTierRecipe implements CraftingRecipe {
 private final ResourceLocation id;
 private final CraftingBookCategory category;
 final int fromTier;
 private final Ingredient center;
 private final Ingredient sides;
 public PackTierRecipe(ResourceLocation id,CraftingBookCategory category,int fromTier,Ingredient center,Ingredient sides){this.id=id;this.category=category;this.fromTier=fromTier;this.center=center;this.sides=sides;}
 @Override public boolean matches(CraftingContainer grid,net.minecraft.world.level.Level level){return grid.getWidth()==3&&grid.getHeight()==3&&matched(grid);}
 private boolean matched(CraftingContainer grid){ItemStack pack=grid.getItem(1);if(!(pack.getItem() instanceof PackItem)||PackData.tierOf(pack)!=fromTier)return false;if(!center.test(grid.getItem(4))||!sides.test(grid.getItem(3))||!sides.test(grid.getItem(5))||!grid.getItem(7).is(Items.IRON_INGOT))return false;for(int i:new int[]{0,2,6,8})if(!grid.getItem(i).is(Items.LEATHER))return false;return true;}
 @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){ItemStack pack=grid.getItem(1);if(!(pack.getItem() instanceof PackItem))return ItemStack.EMPTY;ItemStack out=pack.copy();out.setCount(1);CompoundTag root=out.getOrCreateTag();CompoundTag body=root.getCompound("Pack");body.putInt("Tier",Math.min(4,fromTier+1));root.put("Pack",body);return out;}
 @Override public boolean canCraftInDimensions(int width,int height){return width>=3&&height>=3;}
 @Override public ItemStack getResultItem(RegistryAccess access){ItemStack stack=new ItemStack(AdventurersPack.PACK.get());CompoundTag body=new CompoundTag();body.putInt("Tier",Math.min(4,fromTier+1));stack.getOrCreateTag().put("Pack",body);return stack;}
 @Override public ResourceLocation getId(){return id;}
 @Override public RecipeSerializer<?> getSerializer(){return AdventurersPack.TIER_RECIPE.get();}
 @Override public CraftingBookCategory category(){return category;}
 @Override public NonNullList<Ingredient> getIngredients(){NonNullList<Ingredient> list=NonNullList.withSize(9,Ingredient.EMPTY);for(int i:new int[]{0,2,6,8})list.set(i,Ingredient.of(Items.LEATHER));com.google.gson.JsonObject json=new com.google.gson.JsonObject();json.addProperty("type","forge:partial_nbt");json.addProperty("item","adventurerspack:adventurers_pack");json.addProperty("nbt","{Pack:{Tier:"+fromTier+"}}");list.set(1,fromTier==0?Ingredient.of(AdventurersPack.PACK.get()):Ingredient.fromJson(json));list.set(3,sides);list.set(4,center);list.set(5,sides);list.set(7,Ingredient.of(Items.IRON_INGOT));return list;}
 public static class Serializer implements RecipeSerializer<PackTierRecipe> {
  @Override public PackTierRecipe fromJson(ResourceLocation id,JsonObject json){int from=GsonHelper.getAsInt(json,"from");Ingredient center=Ingredient.fromJson(json.get("center"));Ingredient sides=Ingredient.fromJson(json.get("sides"));return new PackTierRecipe(id,category(GsonHelper.getAsString(json,"category","equipment")),from,center,sides);}
  @Override public PackTierRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf){int from=buf.readVarInt();Ingredient center=Ingredient.fromNetwork(buf);Ingredient sides=Ingredient.fromNetwork(buf);return new PackTierRecipe(id,buf.readEnum(CraftingBookCategory.class),from,center,sides);}
  @Override public void toNetwork(FriendlyByteBuf buf,PackTierRecipe recipe){buf.writeVarInt(recipe.fromTier);recipe.center.toNetwork(buf);recipe.sides.toNetwork(buf);buf.writeEnum(recipe.category);}
  private static CraftingBookCategory category(String name){return switch(name){case "building"->CraftingBookCategory.BUILDING;case "redstone"->CraftingBookCategory.REDSTONE;case "misc"->CraftingBookCategory.MISC;default->CraftingBookCategory.EQUIPMENT;};}
 }
}
