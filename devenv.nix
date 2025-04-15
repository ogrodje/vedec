{ pkgs, lib, config, inputs, ... }:

{
  name = "vedec";
  packages = [
  	pkgs.jq
	pkgs.jnv
	pkgs.k9s
	pkgs.kubectx
  ];

  languages.scala = {
	enable = true;
	package = pkgs.scala_3;
	sbt.enable = true;
  };

  enterShell = ''
  	echo "Entering..."
  	type javac && type sbt

	alias k='microk8s kubectl'
    kubens vedec-prod
  '';

  enterTest = ''
    sbt test
  '';
}
