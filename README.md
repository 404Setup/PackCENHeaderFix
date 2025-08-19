# PackCENHeaderFix

There is no longer an error invalid CEN header (bad entry name or comment)

It solves this problem by replacing Java Zip implementation with Apache Commons Compress implementation.

## Example Story

You downloaded a ResourcePack from a RP author. You put it in your RP folder and tried to load it as usual, but you don't see it in the RP selector.

Instead, Minecraft prints the following error in the log:

![136fae32511da386f54de944925e101fee8c6527.png](https://cdn.modrinth.com/data/7zCj0OEn/images/136fae32511da386f54de944925e101fee8c6527.png)

It doesn't appear in Mojira, precisely because it's a bug that only appears under very extreme conditions. I haven't seen it in the past few years of the game, and only very, very few RP authors have accidentally allowed their RP to be contaminated by this bug.

The cause of this bug is unknown, but it only affects the Java Zip implementation. RPs that experience this bug can still be force-loaded (although you won't be able to find them in the selector), but I think it's better to try to fix it.

## About API
It's intended for use with another mod of mine, which will rely on this one for additional functionality.

If you want to do something with it, my advice is don't, I don't want it to behave unexpectedly.

## How to build?
After the clone repository, be sure to use the `./gradlew clean build` command to build,
rather than executing any of them separately.

After the build is completed, a jars folder will be generated in the root directory,
where the final build results will be stored.

## For Modpack
If you comply with the license, you can use it freely for Modpack.

Modpacks that redistribute Minecraft game body (i.e. packages that package the entire Minecraft game including Mod files, Config, ShaderPacks, ResourcePacks, Library and launcher into a whole zip file) are not allowed to use this mod.

## License

This work has a restrictive license in addition to the original license to prevent some unexpected behavior, see [404Setup Public License](https://github.com/404Setup/404Setup/blob/main/LICENSE.md)