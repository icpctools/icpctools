java_binary(
    name = "resolver",
    main_class = "org.icpc.tools.resolver.Resolver",
    srcs = glob(["**/*.java"]),
    deps = [
        "//ContestModel/src:contestModel",
        "//ContestModel/lib:svgSalamander",
        "//PresContest/src:presentations",
        "//PresCore/src:presentCore",
        "//SWTLauncher/lib:swt",
        "//SWTLauncher/src:swtLauncher",
    ],
    resources = ["org/icpc/tools/resolver/messages.properties"] +
                glob(["images/*.png"]),
)
