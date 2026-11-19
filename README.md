# openadapt

**openadapt** is a clean, standalone Java 25 source reconstruction of the **Adapt v3 (`3.0.4`)** client mod for **Minecraft 26.3 (Fabric)**. All post-build obfuscation and bytecode virtualization layers have been statically reversed back into readable, idiomatic Java source (`269` `.java` files / `367` classes), and all vendor licensing, hardware fingerprinting, and telemetry hooks have been disconnected so the client builds and runs completely offline.

> [!IMPORTANT]
> **Official Mirrors & Anti-DMCA Backup**
> - **GitHub**: https://github.com/yxzeroo/openadapt
> - **Anti-DMCA Backup Mirror**: https://deobf.su/yxzero/openadapt
>
> Please **bookmark and save** https://deobf.su/yxzero/openadapt (or clone a local mirror) in case the GitHub repository is ever taken down via DMCA.

---

## Tech Stack & Versions

| Component | Version |
|---|---|
| **Minecraft** | `26.3` (Mojang unobfuscated release) |
| **Mod Loader** | Fabric Loader `0.20.1` |
| **Fabric API** | `0.166.2+26.3` |
| **Java** | JDK `25` (`--release 25`, classfile `69.0`) |
| **Build System** | Gradle `9.7.1` + Fabric Loom `1.18.2` |

---

## Protections Used & How Each Was Defeated

> [!NOTE]
> **They actually tried to use a VM... in JAVA. LOL.**
>
> Java *already* runs on a stack-based Virtual Machine, and their big-brain protection idea (`shroud.runtime.Vm`) was to write a *second* stack-based VM **in pure Java** on top of it — boxing every single `int` and `float` into `Object[]` arrays inside a giant `switch` loop that wasn't even a custom ISA, just a 1-to-1 shuffled permutation of standard JVM opcodes `0..191` sitting right there in an unobfuscated Java 8 class! You can literally decompile the entire VM interpreter in two seconds, invert the 256-byte lookup table, and dump all `741` "virtualized" methods straight back to clean Java 25 bytecode with **zero** information loss. Paying 50x interpreter overhead in a Minecraft render loop just to lose to a 200-line static script is comedy gold. Even better, their class renamer walked the source folders in alphabetical order before assigning IDs — leaking the exact sort order of every package and class in the project.

The shipped JAR was protected by a multi-layered custom JVM obfuscator and virtualizer (`shroud`). Every layer was unwrapped **100% statically** using custom OW2 ASM, dataflow, and `javac` AST tooling:

1. **64-bit LCG String Encryption (`Ô`)**
   - *Protection*: `1,631` string literals were stripped from classes and replaced with indexed calls into a packed UTF-8 blob decrypted at runtime via a 64-bit Linear Congruential Generator (LCG) XOR stream.
   - *Defeat*: Reversed the LCG multiplier/increment constants from `<clinit>`, precomputed the entire string table statically, and replaced every decryption call with a standard `LDC` string constant.

2. **`InvokeDynamic` Call-Site Hiding (`Ψ`)**
   - *Protection*: `11,737` static, virtual, and interface method calls were replaced with `INVOKEDYNAMIC` instructions whose bootstrap method decrypted `"owner\0name\0descriptor"` triplets and bound `MethodHandles` into `MutableCallSite`s at runtime (with class/member visibility widened to `public`).
   - *Defeat*: Decoded the keyed call-site table statically, resolved target class vs. interface ownership across the application, Minecraft, Fabric, and JDK 25 classpaths, and rewrote all `11,737` sites back to direct `INVOKESTATIC`, `INVOKEVIRTUAL`, and `INVOKEINTERFACE` instructions.

3. **Custom Stack-VM Bytecode Virtualization (`shroud.runtime.Vm` & `Λ0..Λ3`)**
   - *Protection*: `741` application methods were gutted and replaced with `Vm.exec(programId, args)` stubs interpreted by a custom stack-based virtual machine — written in pure Java, running *inside* the JVM. Yes, they genuinely shipped a stack VM inside a stack VM: boxing every primitive into `Object[]` arrays to execute a shuffled 1-to-1 copy of standard JVM opcodes (`0..191`) over a ZLIB-compressed blob (`237,964` chars across 4 constant-pool carrier classes) with `6,006` LCG-encrypted member references. All of the runtime overhead of interpreting Java in Java, with zero actual ISA translation — so the entire interpreter was trivially invertible in ~200 lines of ASM.
   - *Defeat*: Statically inflated the VM binary image, inverted the 256-entry opcode permutation table 1-to-1 back onto real JVM instructions, reconstructed per-method exception handler tables, and lifted all `741` virtualized methods straight back into native Java 25 bytecode with zero information loss.

4. **Mixed Boolean-Arithmetic (MBA) Constant Obfuscation**
   - *Protection*: `7,152` `int` and `long` constants were expanded into multi-instruction `(A ^ B) - C + C` sequences to foil constant-folding and decompilers.
   - *Defeat*: Pattern-matched and folded all `int` and `long` MBA instruction sequences back into single constant loads.

5. **Opaque Predicates & Dead-Code Injection**
   - *Protection*: `5,351` fake conditional jumps seeded by method-entry local guard slots were injected to create bogus control-flow edges into unreachable junk bytecode.
   - *Defeat*: Tracked constant local guard slots from method entry, simplified always-taken predicates to unconditional jumps, pruned unreachable basic blocks via control-flow reachability analysis, and recomputed JVM `StackMapTable` frames.

6. **Unicode Confusable Renaming, Collision Traps & Stripped Generics/Debug Info**
   - *Protection*: `318` classes, `1,897` fields, and `2,121` methods were renamed to 2-character Greek/Cyrillic confusables (including case-insensitive filename collisions targeting macOS/Windows filesystems), while generic `Signature` and `LocalVariableTable` attributes were stripped. Ironically, the obfuscator walked the original class tree in deterministic ASCII path order before assigning sequential IDs, leaking the exact alphabetical position of every single package and class.
   - *Defeat*: Extracted entries using collision-safe indexing, exploited `shroud`'s preserved recursive directory/ASCII sort-order invariant alongside `51` unobfuscated Mixin/Record/Shader anchors to recover the original package and class hierarchy, reconstructed erased generics via bytecode dataflow inference, restored inner-class access flags from `InnerClasses` metadata, decompiled with Vineflower `1.12.0`, and renamed all `3,083` remaining method/lambda parameters using JDK 25's `JavacTask` AST symbol resolver.

---

## Vendor & Telemetry Isolation

All vendor-side licensing, hardware fingerprinting (`DeviceId`), Ed25519 launch-ticket/heartbeat checks (`LaunchGuard`), online player tracking (`AdaptUsers`, `OnlineCommand`), and remote chat polling (`ChatService`) are completely disconnected from all entrypoints and UI screens (`// VENDOR-DISABLED:`):
- **No launcher or license ticket required** — launches directly from Gradle or Fabric Loader.
- **Zero hardware/OS fingerprinting** — `DeviceId` (`ioreg`, `MachineGuid`, `/etc/machine-id`) is unreachable.
- **Zero vendor network traffic** — no connections are made to `adaptclient.com`.

---

## Project Layout

```text
src/main/
├── java/dev/adapt/v3/
│   ├── Adapt.java / AdaptClient.java   # Core lifecycle & Fabric client entrypoint
│   ├── account/                        # Local Minecraft / Microsoft alt-account manager
│   ├── binding/                        # Keyboard & mouse keybind registry
│   ├── command/                        # In-game '.' chat commands (.help, .bind, .toggle, .config, .hud)
│   ├── config/                         # JSON profile serialization (adaptv3.json & configs/)
│   ├── event/                          # MethodHandle CallSite event bus & event types
│   ├── feature/
│   │   ├── helper/                     # Rotation, raytrace, movement, slot, swing, mouse & target helpers
│   │   ├── module/                     # 42 modules across Combat, Movement, Utility, Visual, World & HUD
│   │   └── property/                   # Typed configurable properties (Boolean, Number, Enum, MultiBoolean, Color)
│   ├── hud/                            # Draggable/scalable HUD element manager
│   ├── mixin/                          # 46 SpongePowered Mixins, Accessors & Invokers
│   ├── notification/                   # Toast notification system
│   ├── security/ & chat/               # Dormant, unreferenced vendor classes (preserved for reference)
│   ├── ui/                             # Dashboard, HUD Editor, Main Menu, custom font & GLSL blur/glass renderer
│   └── util/                           # Spring/easing animations, math, player, packet & projection utilities
└── resources/
    ├── fabric.mod.json
    ├── adaptv3.mixins.json
    └── assets/adaptv3/                 # Fonts, icon, post-processing effects & GLSL fragment shaders
```

---

## Building & Running

Requires **JDK 25** installed on your machine.

```bash
# Compile and build build/libs/adaptv3-3.0.4.jar
./gradlew clean build

# Launch Minecraft 26.3 with openadapt in development mode
./gradlew runClient
```

- Press **Right Shift** (or open from the main menu) in-game to open the **Dashboard**, configure modules, edit **HUD Layout**, or manage **Configs**.
