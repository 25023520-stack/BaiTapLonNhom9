# BaiTapLonNhom9

## Cau truc source code

Project duoc to chuc theo huong client-server va tach ro giao dien, nghiep vu, model dung chung:

```text
src/
  main/
    java/com/auction/system/
      client/
        App.java                    # bootstrap client, chon DEMO/SEPARATE mode
        Launcher.java               # entry point JavaFX
        context/
          AppContext.java           # session client, socket client, current user
        controller/
          LoginController.java      # controller man hinh dang nhap
          RegisterController.java   # controller man hinh dang ky
          AuctionController.java    # controller man hinh dau gia
        network/
          AuctionClient.java        # TCP client giao tiep voi server
      server/
        ServerMain.java             # entry point server
        controller/
          AuthController.java       # xu ly request auth
          AuctionController.java    # xu ly request dau gia
        manager/
          AuthManager.java          # nghiep vu tai khoan
          UserManager.java          # quan ly user
          ItemManager.java          # quan ly item
          AuctionManager.java       # nghiep vu dau gia
        network/
          AuctionServer.java        # TCP server
          ClientHandler.java        # xu ly tung client
        dao/
          UserDAO.java              # truy cap du lieu user
        database/
          Database.java             # ket noi/co so du lieu
      model/
        user/                       # User, Admin, Seller, Bidder
        item/                       # Item, Electronics, Art, Vehicle
        auction/                    # Auction, Bid, AuctionStatus
      common/
        payload/                    # payload client-server
        json/                       # GsonProvider dung chung
      factory/
        ItemFactory.java            # Factory Method tao item
      exception/                    # custom exception
    resources/com/auction/system/client/view/
      Login.fxml
      Register.fxml
      Auction.fxml
      Styles.css
  test/
    java/com/auction/system/...
```

## Doi chieu voi yeu cau de bai

- `client/` + `resources/.../view/`: phan Frontend JavaFX, FXML, MVC.
- `server/`: phan Backend, manager, controller, TCP socket server.
- `model/`: cac entity dung chung cho client va server.
- `common/payload/`: giao thuc truyen du lieu client-server.
- `factory/`: ap dung Factory Method.
- `exception/`: custom exception cho nghiep vu.

## Cach chay

### Chay server rieng

```powershell
mvn -DskipTests package
java -jar target/server.jar
```

### Chay client JavaFX

```powershell
mvn --% -DskipTests -Djavafx.run.jvmArgs="-Dauction.client.mode=SEPARATE -Dauction.server.host=127.0.0.1 -Dauction.server.port=5050" javafx:run
```

Neu muon mo 2 client de test realtime, chay lenh tren trong 2 terminal khac nhau sau khi server da len.

## Luu y ve cau truc

- Khong de file IDE trong `src/`.
- Khong de file env/tooling rieng cua IDE trong `src/main/resources/.../view/`.
- Logic dau gia nam o `server/manager/`, UI chi goi request qua `client/network/`.
