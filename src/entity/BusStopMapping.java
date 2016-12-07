package entity;

import java.util.HashMap;

/*
	Wrong Mapping:
	AS7(AS7) -> 16009(Kent Ridge Ter)   (.240km)
	BIZ2(BIZ 2) -> 16069(Heng Mui Keng Terr)   (.348km)
	BUKITTIMAH-BTC2(BTC - Oei Tiong Ham Building) -> 41021(Botanic Gdns Stn)   (.419km)
	COM2(COM2 (CP13)) -> 16181(Ctrl Lib)   (.292km)
	LT13(LT13) -> 16009(Kent Ridge Ter)   (.102km)
	HSSML-OPP(Opp HSSML) -> 16069(Heng Mui Keng Terr)   (.301km)
	NUSS-OPP(Opp NUSS) -> 16071(Opp Pasir Panjang PO)   (.267km)
	PGP12-OPP(Opp PGP Hse No 12) -> 18311(Blk S12)   (.417km)
	PGP12(PGP Hse No 12) -> 18311(Blk S12)   (.439km)
	PGP14-15(PGP Hse No 14 and No 15) -> 16049(Westvale)   (.438km)
	PGP7(PGP Hse No 7) -> 16049(Westvale)   (.454km)
	PGP(PGPR) -> 16101(Inst Of Micro-Electronics)   (.218km)
	PGPT(Prince George's Park) -> 16101(Inst Of Micro-Electronics)   (.308km)
	UTown(University Town) -> 16161(Museum)   (.294km)
	LT13-OPP(Ventus (Opp LT13)) -> 16139(Bef Kent Ridge Ter)   (.086km)
	
	Correct mapping:
	CGH(College Green Hostel) -> 41029(Opp Botanic Gdns Stn)   (.000km)
	BG-MRT(Botanic Gardens MRT) -> 41021(Botanic Gdns Stn)   (.049km)
	
	CENLIB(Central Library) -> 16181(Ctrl Lib)   (.042km)
	COMCEN(Computer Centre) -> 16189(Computer Ctr)   (.017km)
	KR-BT(Kent Ridge Bus Terminal) -> 16009(Kent Ridge Ter)   (.020km)
	KR-MRT(Kent Ridge MRT) -> 18331(Kent Ridge Stn)   (.010km)
	MUSEUM(Museum) -> 16161(Museum)   (.015km)
	S17(S17) -> 18309(Blk S17)   (.042km)
	UHALL(UHall) -> 18311(Blk S12)   (.018km)
	STAFFCLUB(University Health Centre) -> 18329(University Health Ctr)   (.010km)
	YIH(YIH) -> 16171(Yusof Ishak Hse)   (.024km)
	STAFFCLUB-OPP(Opp University Health Centre) -> 18321(Opp University Health Ctr)   (.014km)
	UHALL-OPP(Opp UHall) -> 18319(Opp University Hall)   (.019km)
	RAFFLES(Raffles Hall) -> 16169(NUS Raffles Hall)   (.012km)
	YIH-OPP(Opp YIH) -> 16179(Opp Yusof Ishak Hse)   (.019km)
	KR-MRT-OPP(Opp Kent Ridge MRT) -> 18339(Opp Kent Ridge Stn)   (.017km)
	LT29(LT29) -> 18301(Lim Seng Tjoe Bldg (LT 27))   (.025km)
	KV(Kent Vale) -> 16151(The Japanese Pr Sch)   (.176km)
	BLK-EA-OPP(Opp Block EA) -> 16159(NUS Fac Of Engrg)   (.014km)
	
	Unable to do cut off due to KV->Jap Pri Sch too far off
 */
public class BusStopMapping {
	
	public static HashMap<String, String> NUSToPublic;
	public static HashMap<String, String> NTUToPublic;
	static {
		//Initialize HashMaps
		NUSToPublic = new HashMap<String, String>(20);
		NTUToPublic = new HashMap<String, String>(30);
		
		//NUS Mappings
		NUSToPublic.put("CGH", "41029");
		NUSToPublic.put("BG-MRT", "41021");
		NUSToPublic.put("CENLIB", "16181");
		NUSToPublic.put("COMCEN", "16189");
		NUSToPublic.put("KR-BT", "16009");
		NUSToPublic.put("KR-MRT", "18331");
		NUSToPublic.put("MUSEUM", "16161");
		NUSToPublic.put("S17", "18309");
		NUSToPublic.put("UHALL", "18311");
		NUSToPublic.put("STAFFCLUB", "18329");
		NUSToPublic.put("YIH", "16171");
		NUSToPublic.put("STAFFCLUB-OPP", "18321");
		NUSToPublic.put("UHALL-OPP", "18319");
		NUSToPublic.put("RAFFLES", "16169");
		NUSToPublic.put("YIH-OPP", "16179");
		NUSToPublic.put("KR-MRT-OPP", "18339");
		NUSToPublic.put("LT29", "18301");
		NUSToPublic.put("KV", "16151");
		NUSToPublic.put("BLK-EA-OPP", "16159");
		
		//NTU Mappings (stops that start with 63XXX has no public buses) 
		//=================== Red
		NTUToPublic.put("378224", "27211");
		//NTUToPublic.put("382995", "63741");
		NTUToPublic.put("378227", "27231");
		NTUToPublic.put("378228", "27241");
		NTUToPublic.put("378229", "27251");
		NTUToPublic.put("378230", "27261");
		NTUToPublic.put("378233", "27281");
		NTUToPublic.put("378237", "27311");
		NTUToPublic.put("382998", "27209");
		NTUToPublic.put("378202", "27011");
		NTUToPublic.put("378204", "27031");
		//=================== Blue
		NTUToPublic.put("378225", "27219");
		//NTUToPublic.put("382999", "63713");
		//NTUToPublic.put("378203", "63717");
		//NTUToPublic.put("378222", "63721");
		//NTUToPublic.put("383003", "63723");
		//NTUToPublic.put("378234", "63727");
		//NTUToPublic.put("383004", "63731");
		//NTUToPublic.put("383006", "63701");
		//NTUToPublic.put("383009", "63703");
		//NTUToPublic.put("383010", "63707");
		//NTUToPublic.put("378226", "63711");
		//=================== Green
		NTUToPublic.put("377906", "22529");
		NTUToPublic.put("378233", "27281");
		NTUToPublic.put("378237", "27311");
		//NTUToPublic.put("383011", "63735");
		//NTUToPublic.put("383013", "63737");
		//NTUToPublic.put("383014", "63725");
		//=================== Brown
		NTUToPublic.put("377906", "22529");
		NTUToPublic.put("378233", "27281");
		NTUToPublic.put("378237", "27311");
		//NTUToPublic.put("383011", "63735");
		//NTUToPublic.put("383013", "63737");
		NTUToPublic.put("378207", "27061");
		NTUToPublic.put("378224", "27211");
		NTUToPublic.put("383015", "27221");
		//NTUToPublic.put("382995", "63741");
		NTUToPublic.put("378227", "27231");
		NTUToPublic.put("378228", "27241");
		NTUToPublic.put("378229", "27251");
		NTUToPublic.put("378230", "27261");
		//NTUToPublic.put("383018", "63747");
	}

	public static String getValue(String key) {
		if (NUSToPublic.containsKey(key)) {
			return NUSToPublic.get(key);
		} else if (NTUToPublic.containsKey(key)) {
			return NTUToPublic.get(key);
		} else {
			return null;
		}
	}
}
