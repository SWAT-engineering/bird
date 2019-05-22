module lang::bird::tests::GenerateAll

import lang::bird::Generator2Nest;

list[str] BIRD_FILE_NAMES = [
	"linkedList1",
	"nesting_and_cycles",
	"while1",
	"varint",
	"JPEG",
	"tie1",
	"PNG"
];

void main() {
	for (str birdFileName <- BIRD_FILE_NAMES) {
		compileBirdTo(birdFileName, |project://nest/test/engineering/swat/nest/examples/formats/bird_generated/<birdFileName>$.java|);
	}
}