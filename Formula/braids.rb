class Braids < Formula
  desc "Autonomous background project management for OpenClaw agents"
  homepage "https://github.com/slagyr/braids"
  url "https://github.com/slagyr/braids.git", tag: "v0.1.0"
  license "MIT"

  depends_on "borkdude/brew/babashka"
  depends_on "beads"

  def install
    # Install the braids skill source
    libexec.install Dir["*"]

    # Create a wrapper script that runs bb braids from the installed location
    (bin/"braids").write <<~EOS
      #!/usr/bin/env bash
      exec bb --config "#{libexec}/bb.edn" braids "$@"
    EOS
  end

  test do
    assert_match "braids", shell_output("#{bin}/braids help")
  end
end
