package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public interface ObjectTag {

    /*
     * ObjectTags should contain these two static methods, of which valueOf contains a valid
     * annotation for ObjectFetcher
     *
     * public static ObjectTag valueOf(String string);
     *
     * valueOf() should take a string representation of the object, preferably with a valid object
     * notation (x@), and turn it into a new instance of the ObjectTag. Care has to be taken to
     * ensure that it is compatible with the tag system (ie. no periods (.) outside of square brackets),
     * and other parts of Denizen.
     *
     * Since your object may be using the ObjectTag Attributes System, valueOf should take that into
     * account as well.
     *
     *
     * public static boolean matches()
     *
     * matches() should use some logic to determine if a string is in the proper format to possibly
     * return a non-null valueOf() call.
     *
     */

    /**
     * Retrieves the dScript argument prefix. ObjectTags should provide a default
     * prefix if nothing else has been specified.
     *
     * @return the prefix
     */
    String getPrefix();

    /**
     * <p>Gets a standard dB representation of this argument. All ObjectTags should follow
     * suit.</p>
     * <p/>
     * Example: <br/>
     * <tt>
     * Location='x,y,z,world'
     * Location='unique_location(x,y,z,world)'
     * </tt>
     *
     * @return the debug information
     */
    default String debug() {
        return "<G>" + getPrefix() + "='<Y>" + debuggable() + "<G>'  ";
    }

    /**
     * Gets a debuggable format of the object. Like identify, but for console output.
     */
    default String debuggable() {
        return identify();
    }

    // <--[language]
    // @name Unique Objects vs Generic Objects
    // @group Object System
    // @description
    // There are a lot of object types in the Denizen object system, and not all of them behave the same way.
    // It can be useful to separate object types into categories to better understand how objects work, and how Denizen as a whole works.
    // While there are some hardlined separations, there are also some generalizations that don't necessarily hold exactly, but are still helpful.
    // One such generalization is the separation between Unique and Generic object types.
    //
    // A UNIQUE object is the way you might assume all objects are.
    // Unique objects most notably include EntityTag objects and the derivative NPCTag / PlayerTag objects.
    // An entity object identifies in a form like 'e@<uuid>', where '<uuid>' is some unique ID that looks something like 'abc123-4d5e6f'.
    // This ID is randomly generated by the Minecraft server and is used to identify that one entity in the world separately from any other.
    // 'e@abc123' is not "a creeper" to the engine, it is "that specific creeper over there".
    // An object that is unique must have some way to specify the exact single instance of it in the world, like the UUID used by entities.
    //
    // A GENERIC object can be said to be a 'description' of a unique object.
    // A generic form of an EntityTag might look like 'e@creeper'.
    // Instead of "that specific creeper over there", it is instead "the concept of a creeper mob".
    // There is no way for the engine to read only the word 'creeper' and find out which specific creeper in the world it came from...
    // it could be any of them, there's often hundreds of creepers spawned somewhere in the world at any time.
    // Objects like items and materials are always generic. There is no unique identifier for any item, there is only the description.
    // An item usually looks something like 'i@stick'.
    // ItemTag objects can include more detail, like 'i@stick[lore=hi;display_name=My Stick;enchantments=sharpness,5]'...
    // but this is still just a description, and there could still be many items out there that match this description.
    //
    // The consequences of this mostly relate to:
    // - How you adjust an object (eg change the lore of an item, or teleport an entity).
    // For example: you can't teleport the generic concept of a creeper, but you can certainly teleport a specific single creeper in the world.
    // - How reliable tags on the object are.
    // For example: the result of 'ItemTag.lore' on the item 'i@stick[lore=hi]' will always be 'hi' (because ItemTag are generic),
    // but the result of 'EntityTag.location' on the entity 'e@abc123' will change every tick as the entity moves,
    // or even become invalid if the entity dies (because that EntityTag is unique).
    //
    // Here's where the separation gets muddy:
    // First, as mentioned, an EntityTag can either be unique ('e@abc123', 'n@42', etc.) OR generic ('e@creeper', 'e@zombie[custom_name=Bob]', etc).
    // Second, some object types exhibit a bit of both.
    //
    // For example, a LocationTag refers to a unique block in the world (like, 'l@1,2,3,world' is always that specific block at that position),
    // but also is a generic object in terms of the location object itself - if for example you want to change the angle of a location,
    // you have to essentially 'create' a new location, that is an exact copy of the previous one but with a new yaw or pitch value.
    // -->

    /**
     * Determines if this argument object is unique. This typically stipulates
     * that this object has been named, or has some unique identifier that
     * Denizen can use to recall it.
     *
     * @return true if this object is unique, false if it is a 'singleton generic argument/object'
     */
    boolean isUnique();

    /**
     * Returns the string type of the object. This is fairly verbose and crude, but used with
     * a basic dScriptArg attribute.
     *
     * @return a straight-up string description of the type of dScriptArg. ie. ListTag, LocationTag
     */
    String getObjectType();

    /**
     * Gets an ugly, but exact, string representation of this ObjectTag.
     * While not specified in the ObjectTag Interface, this value should be
     * able to be used with a static valueOf(String) method to reconstruct the object.
     *
     * @return a single-line string representation of this argument
     */
    String identify();

    default String savable() {
        return identify();
    }

    /**
     * Gets an overall string representation of this ObjectTag.
     * This should give the basic jist of the object being identified, but
     * won't include the exactness that identify() uses.
     * <p/>
     * <code>
     * Example: i@gold_sword     vs.    i@gold_sword[display_name=Shiny Sword]
     * ^                       ^
     * +--- identifySimple()   +--- identify()
     * </code>
     * <p/>
     * This may produce the same results as identify(), depending on the complexity
     * of the object being identified.
     *
     * @return a single-line, 'simple' string representation of this argument
     */
    String identifySimple();

    /**
     * Create a duplicate of this object, if needed. Primarily for object types where this could specifically matter.
     * Immutable ObjectTags do not need to implement.
     */
    default ObjectTag duplicate() {
        return this;
    }

    /**
     * If any fixes need to be handled after properties are applied to an object, they should be handled here.
     */
    default ObjectTag fixAfterProperties() {
        return this;
    }

    /**
     * Sets the prefix for this argument, otherwise uses the default.
     *
     * @return the ObjectTag
     */
    ObjectTag setPrefix(String prefix);

    default Class<? extends ObjectTag> getObjectTagClass() {
        return getClass();
    }

    default <T extends ObjectTag> T asType(Class<T> type, TagContext context) {
        return CoreUtilities.asType(this, type, context);
    }

    /**
     * Gets a specific attribute using this object to fetch the necessary data.
     *
     * @param attribute the name of the attribute
     * @return a string result of the fetched attribute
     */
    default ObjectTag getObjectAttribute(Attribute attribute) {
        String res = getAttribute(attribute);
        return res == null ? null : new ElementTag(res);
    }

    default String getAttribute(Attribute attribute) {
        return CoreUtilities.stringifyNullPass(getObjectAttribute(attribute));
    }

    /**
     * Get the "next object type down" - by default, an ElementTag of identify(), but can be different in some cases (eg a Player's next type down is Entity).
     * Should never be null.
     */
    default ObjectTag getNextObjectTypeDown() {
        return new ElementTag(identify());
    }

    /**
     * Optional special dynamic tag handling
     */
    default ObjectTag specialTagProcessing(Attribute attribute) {
        return null;
    }
}
