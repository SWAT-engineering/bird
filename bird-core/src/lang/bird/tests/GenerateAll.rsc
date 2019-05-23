module lang::bird::tests::GenerateAll

import lang::bird::Generator2Nest;
import IO;

list[str] BIRD_FILE_NAMES = [
	"linkedList1",
	"nesting_and_cycles",
	"while1",
	"varint",
	"JPEG",
	"tie1",
	"PNG",
	"crc",
	"typeParameters1",
	"fatLikeNesting1"
];

void main() {
	list[str] msgs = [];
	for (str birdFileName <- BIRD_FILE_NAMES) {
		try {
			compileBirdTo(birdFileName, |project://nest/test/engineering/swat/nest/examples/formats/bird_generated/<birdFileName>$.java|);
		}
		catch 
			_: {
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