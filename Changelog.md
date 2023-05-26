# 2.0.0.1 Compatibility improvements

- Remove Title Screen Listener to initialize cloud settings
- Use mixin into Minecraft to initialize cloud settings

# 2.0.0.0 Another Rewrite?

It's been 6 Months since the last rewrite which brought us a suspicious looking login screen and a login cert file and
i instantly regretted making those. 

That's why I removed them completely and switched to a new authorization concept
which uses the Microsoft Access Token to identify a user using the MinecraftServices.com API. This is basically the same
way as Minecraft uses it to verify you're not a cracked minecraft player when joining a Server.

So here are the good news

- Improved load up speed due to async option requesting
- No log in required anymore

