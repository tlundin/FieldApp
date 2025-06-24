package com.teraim.fieldapp.utils;
import com.google.gson.*;
import com.teraim.fieldapp.dynamic.blocks.*; // Import all block classes from the package
import java.lang.reflect.Type;

/**
 * A custom deserializer for the abstract Block class. It uses the 's_type' field
 * in the JSON to determine which concrete subclass of Block to instantiate.
 */
public class BlockDeserializer implements JsonDeserializer<Block> {

    @Override
    public Block deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // 1. Get the type property from the JSON object.
        JsonElement typeElement = jsonObject.get("s_type");
        if (typeElement == null || typeElement.isJsonNull()) {
            throw new JsonParseException("Block object is missing the 's_type' property: " + jsonObject);
        }

        String blockType = typeElement.getAsString();

        // 2. Use the type to determine which class to deserialize into.
        Class<? extends Block> blockClass;
        switch (blockType) {
            case "AddEntryToFieldListBlock":
                blockClass = AddEntryToFieldListBlock.class;
                break;
            case "AddFilter":
                blockClass = AddFilter.class;
                break;
            case "AddGisFilter":
                blockClass = AddGisFilter.class;
                break;
            case "AddGisLayerBlock":
                blockClass = AddGisLayerBlock.class;
                break;
            case "AddGisPointObjects":
                blockClass = AddGisPointObjects.class;
                break;
            case "AddSumOrCountBlock":
                blockClass = AddSumOrCountBlock.class;
                break;
            case "AddVariableToEntryFieldBlock":
                blockClass = AddVariableToEntryFieldBlock.class;
                break;
            case "AddVariableToEveryListEntryBlock":
                blockClass = AddVariableToEveryListEntryBlock.class;
                break;
            case "AddVariableToListEntry":
                blockClass = AddVariableToListEntry.class;
                break;
            case "BarChartBlock":
                blockClass = BarChartBlock.class;
                break;
            case "BlockAddAggregateColumnToTable":
                blockClass = BlockAddAggregateColumnToTable.class;
                break;
            case "BlockAddColumnsToTable":
                blockClass = BlockAddColumnsToTable.class;
                break;
            case "BlockAddVariableToTable":
                blockClass = BlockAddVariableToTable.class;
                break;
            case "BlockCreateListEntriesFromFieldList":
                blockClass = BlockCreateListEntriesFromFieldList.class;
                break;
            case "BlockCreateTable":
                blockClass = BlockCreateTable.class;
                break;
            case "BlockCreateTableEntriesFromFieldList":
                blockClass = BlockCreateTableEntriesFromFieldList.class;
                break;
            case "BlockCreateTextField":
                blockClass = BlockCreateTextField.class;
                break;
            case "BlockDeleteMatchingVariables":
                blockClass = BlockDeleteMatchingVariables.class;
                break;
            case "BlockGoSub":
                blockClass = BlockGoSub.class;
                break;
            case "ButtonBlock":
                blockClass = ButtonBlock.class;
                break;
            case "ChartBlock":
                blockClass = ChartBlock.class;
                break;
            case "ConditionalContinuationBlock":
                blockClass = ConditionalContinuationBlock.class;
                break;
            case "ContainerDefineBlock":
                blockClass = ContainerDefineBlock.class;
                break;
            case "CoupledVariableGroupBlock":
                blockClass = CoupledVariableGroupBlock.class;
                break;
            case "CreateCategoryDataSourceBlock":
                blockClass = CreateCategoryDataSourceBlock.class;
                break;
            case "CreateEntryFieldBlock":
                blockClass = CreateEntryFieldBlock.class;
                break;
            case "CreateGisBlock":
                blockClass = CreateGisBlock.class;
                break;
            case "CreateImageBlock":
                blockClass = CreateImageBlock.class;
                break;
            case "CreateSliderEntryFieldBlock":
                blockClass = CreateSliderEntryFieldBlock.class;
                break;
            case "CreateSortWidgetBlock":
                blockClass = CreateSortWidgetBlock.class;
                break;
            case "CreateTwoDimensionalDataSourceBlock":
                blockClass = CreateTwoDimensionalDataSourceBlock.class;
                break;
            case "DisplayFieldBlock":
                blockClass = DisplayFieldBlock.class;
                break;
            case "DisplayValueBlock":
                blockClass = DisplayValueBlock.class;
                break;
            case "JumpBlock":
                blockClass = JumpBlock.class;
                break;
            case "LayoutBlock":
                blockClass = LayoutBlock.class;
                break;
            case "MenuEntryBlock":
                blockClass = MenuEntryBlock.class;
                break;
            case "MenuHeaderBlock":
                blockClass = MenuHeaderBlock.class;
                break;
            case "NoOpBlock":
                blockClass = NoOpBlock.class;
                break;
            case "PageDefineBlock":
                blockClass = PageDefineBlock.class;
                break;
            case "RoundChartBlock":
                blockClass = RoundChartBlock.class;
                break;
            case "RuleBlock":
                blockClass = RuleBlock.class;
                break;
            case "SetValueBlock":
                blockClass = SetValueBlock.class;
                break;
            case "StartBlock":
                blockClass = StartBlock.class;
                break;
            case "StartCameraBlock":
                blockClass = StartCameraBlock.class;
                break;
            default:
                throw new JsonParseException("Unknown block type specified in s_type: " + blockType);
        }

        // 3. Delegate the actual deserialization to the default Gson process for the determined class.
        return context.deserialize(jsonObject, blockClass);
    }
}