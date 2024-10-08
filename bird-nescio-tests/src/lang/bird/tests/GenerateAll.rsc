module lang::bird::tests::GenerateAll

extend lang::bird::Generator2Nest;
import lang::bird::Checker;
import IO;

PathConfig getDefaultPathConfig() = pathConfig(
	srcs = [|project://bird-nescio-tests/bird-src/|], libs = [], target = |project://bird-nescio-tests/test/|);

private loc project(loc file) {
   return |project:///|[authority = file.authority];
}


list[str] BIRD_FILE_NAMES = [
	"linkedList1",
	"nesting_and_cycles",
	"while1",
	"varint",
	"JPEG",
	"uvw",
	"x",
	"tie1",
	"PNG",
	"crc",
	"typeParameters1",
	"fatLikeNesting1",
	"A",
	"B",
	"AorB",
	"rep1",
	"network2/PCAP",
	"network2/TCP_IP"
];

void run() {
	list[str] msgs = [];
	PathConfig pcfg = getDefaultPathConfig();
	for (str birdFileName <- BIRD_FILE_NAMES) {
		try {
			compileBirdModule(|project://bird-nescio-tests/bird-src/<birdFileName>.bird|, "engineering.swat.bird.generated", pcfg); 
		}
		catch 
			e: {
				msgs = msgs + ["Error compiling <birdFileName>"];
			}
	}
	if (_ <- msgs) {
		println("------");
		println("Report");
		println("------");
		for (msg <- msgs)
			println(msg);
	} else {
		println("All files compiled successfully");
	}	
}