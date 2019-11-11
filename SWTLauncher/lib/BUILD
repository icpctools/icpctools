java_import(
    name = "swt",
    jars =
        select({
            "@bazel_tools//src/conditions:darwin": [":swt-cocoa-macosx-x86_64.jar"],
            "@bazel_tools//src/conditions:windows": [":swt-win32-win32-x86_64.jar"],
            "//conditions:default": [":swt-gtk-linux-x86_64.jar"],
        }),
    visibility = ["//visibility:public"],
)
