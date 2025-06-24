package com.teraim.fieldapp.utils; // Assuming this is the package for Expressor and its inner classes

import com.google.gson.*;
import com.teraim.fieldapp.utils.Expressor.*; // Import the inner classes from Expressor

import java.lang.reflect.Type;

/**
 * A robust custom deserializer for the abstract EvalExpr class. It uses the
 * structure of the JSON object (the presence of specific keys like 'arg1', 'args', 'myToken', or 'str')
 * to determine which concrete subclass of EvalExpr to instantiate.
 */
public class EvalExprDeserializer implements JsonDeserializer<EvalExpr> {

    @Override
    public EvalExpr deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Class<? extends EvalExpr> exprClass;

        // Determine the class based on the unique properties of each type
        if (jsonObject.has("operator") && jsonObject.has("arg1")) {
            // This is a binary operation, represented by the Convoluted class.
            exprClass = Convoluted.class;
        } else if (jsonObject.has("args")) {
            // This is a function call.
            exprClass = Function.class;
        } else if (jsonObject.has("myToken")) {
            // This represents a variable, number, or literal.
            exprClass = Atom.class;
        } else if (jsonObject.has("str")) {
            // This is a plain text segment outside an expression.
            exprClass = Text.class;
        } else {
            throw new JsonParseException("Cannot determine type for EvalExpr: " + jsonObject);
        }

        // Delegate the actual deserialization to the default Gson process for the determined class.
        return context.deserialize(jsonObject, exprClass);
    }
}