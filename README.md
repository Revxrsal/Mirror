# Mirror
A simple, annotation-based, powerful, flexible yet performant reflection utility.

## Features
 - Ability to "mirror" classes, fields and methods by accessing them reflectively.
 - Well-supported for recursive mirroring (accepts mirrored parameters and return types)
 - Supports access to static fields and methods
 - Supports constructing instances for mirrored types
 - Supports mirroring enum types and their enum constants
 - Ability to infer the appropriate constructor or method from the given parameters (to a really high degree)
 - Uses the modern and fast MethodHandles API introduced in Java 7, and caches MethodHandle instances for later use
 - Supports getters and setters for fields, with access to modify values of `final` ones.
 - Concise and understandable error messages
 - Supports Bukkit, CraftBukkit and NMS mappings and obfuscation.

## Example
- **Accessing a private field**

**Note**: I advise against modifying immutable lists, however this is **purely** for demonstrating purposes. Unless... \*wink*

```java
public interface ImmutableListMirror extends Mirror {  
  
  @MirrorField("array") // an Object[] in RegularImmutableList which stores the data  
  Object[] getData();  
  
}

public static void main(String[] args) {  
  ImmutableList<String> list = ImmutableList.of("Am I truly immutable?", "I think so!");  
//                             ^------------^ returns a RegularImmutableList.  
  System.out.println("Before: " + list);  

  ImmutableListMirror mirror = Mirror.mirrorize(list, ImmutableListMirror.class);  
  mirror.getData()[1] = "Not really!";  
  System.out.println("After: " + list);  
}  
```

Prints:
```
Before: [Am I truly immutable?, I think so!]
After: [Am I truly immutable?, Not really!]
```
<br>

- **Setting a (possibly final) field**

Let's say we have this simple `Cow` class:
```java
public class Cow {  
  
  private final String name;  
  
  public Cow(String name) {  
     this.name = name;  
  }  
  
  public String getName() {  
     return name;  
  }  
}
```
`name` is final, hence we cannot modify it unless we use ugly reflection.

However,
```java
public interface CowMirror extends Mirror {  
  
  @MirrorField("name") // a setter for the "name" field.
  void setName(String name);  
  
}  
  
public static void main(String[] args) {  
  Cow cow = new Cow("Lucy");  
  System.out.println(cow.getName() + ": MOOOOOO");  
  
  CowMirror cowMirror = Mirror.mirrorize(cow, CowMirror.class);  
  cowMirror.setName("Bruce");  
  
  System.out.println(cow.getName() + ": Mooo?");  
}
```

Prints: 
```
Lucy: MOOOOOO
Bruce: Mooo?
```
<br>

* **Reflecting methods**

Back to our `Cow` example, let's say we added this to it:
```java
private void moo() {  
  System.out.println(name + ": MOOOOOO");  
}
```
Method is private, cows only moo when they want to. Unless...
```java
public interface CowMirror extends Mirror {  
  
  void moo();  
  
}  
  
public static void main(String[] args) {  
  Cow cow = new Cow("Lucy");  
  
  CowMirror cowMirror = Mirror.mirrorize(cow, CowMirror.class);  
  cowMirror.moo();  
}
```
We can now get Lucy to moo all day!

<br>

- **Mirroring enums**

Let's say we have this `enum` class:
```java
enum AnimalType {  
  
  WOLF("Woof"),  
  CAT("Meow"),  
  COW("Moo");  
  
  private final String sound;  
  
  AnimalType(String sound) {  
     this.sound = sound;  
  }  
}
```
Since `AnimalType` is package-private, we cannot normally access it. But...
```java
@MirrorEnum  
@MirrorClass("test.AnimalType")  
public interface AnimalTypeEnum extends Mirror {  
  
  @MirrorEnumName("WOLF")  
  Object getWolf(); // Returns AnimalType.WOLF directly  
  
  @MirrorEnumName("CAT")  
  AnimalTypeConst getCat(); // Returns a mirror for AnimalType.CAT  
  
  interface AnimalTypeConst extends Mirror {  
  
	  @MirrorField("sound")  
	  String getSound();  
	  
  }  
}

public static void main(String[] args) {  
  AnimalTypeEnum animalTypeEnum = Mirror.mirrorizeEnum(AnimalTypeEnum.class);  
  System.out.println("Cats say " + animalTypeEnum.getCat().getSound());  
}
```

- **Constructing mirror instances**

Back to our Cow:
```java
class Cow {  
  
  private Cow(String name) {  
	  System.out.println("Welcome to the farm, " + name + " the cow!");  
  }  
}
```
Class is package-private and constructor is private. But we want our own Cow!
```java
@MirrorClass("test.Cow")  
public interface CowMirror extends Mirror {  
  
}  
  
public static void main(String[] args) {  
  CowMirror cow = Mirror.construct(CowMirror.class, "Slam");  
}
```

Prints: 
```
Welcome to the farm, Slam the cow!
```
<br>

- **Static methods and fields**

Let's imagine we have this `Barn` class:
```java
public final class Barn {  
  
  private static void setFarmer(String farmer) {  
	  System.out.println("Our new farmer is " + farmer + "!");  
  }    
}
``` 

Since `setFarmer` is private and static, we do not need an instance to call it. Hence we can obtain a special mirror called a static mirror:

```java
@MirrorClass("net.test.Barn")  
public interface BarnMirror extends Mirror {  
  
  void setFarmer(String farmer);  
  
}  
  
public static void main(String[] args) {  
  BarnMirror barn = Mirror.forStatic(BarnMirror.class);  
  barn.setFarmer("Old McDonald");  
}
```
Note that any attempt to run instance-methods or get instance-fields will throw an exception.

# Disclaimer
This library was highly influenced by [Lucko's `shadow` library](https://github.com/lucko/shadow), however with some improvements and more features.