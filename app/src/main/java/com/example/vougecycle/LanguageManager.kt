package com.example.vougecycle

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** Stores the user's language choice and translates the app's built-in interface text. */
object LanguageManager {
    private const val PREFS = "vougecycle_preferences"
    private const val LANGUAGE = "language"
    const val USER_CONTENT_TAG = "vougecycle_user_content"

    private val englishToZulu = mapOf(
        "Vogue Cycle" to "Vogue Cycle",
        "VougeCycle" to "Vogue Cycle",
        "VougeCycle logo" to "Ilogo ye-Vogue Cycle",
        "Vogue Cycle logo" to "Ilogo ye-Vogue Cycle",
        "Buy vintage · Sell vintage" to "Thenga izingubo ze-vintage · Thengisa izingubo ze-vintage",
        "Buy vintage · Sell vintage · Keep fashion in motion" to "Thenga izingubo ze-vintage · Thengisa izingubo ze-vintage · Gcina imfashini iqhubeka",
        "Keep fashion in motion" to "Gcina imfashini iqhubeka",
        "Sign Up" to "Bhalisa",
        "Enter your details below" to "Faka imininingwane yakho ngezansi",
        "Full Name" to "Igama eligcwele",
        "Full name" to "Igama eligcwele",
        "Username" to "Igama lomsebenzisi",
        "Email address" to "Ikheli le-imeyili",
        "Password" to "Iphasiwedi",
        "Confirm Password" to "Qinisekisa iphasiwedi",
        "Use at least 6 characters, one number and one capital letter" to "Sebenzisa okungenani izinhlamvu eziyisi-6, inombolo eyodwa nohlamvu olulodwa olukhulu",
        "Have an account?" to "Usunayo i-akhawunti?",
        "Log In" to "Ngena ngemvume",
        "Log out" to "Phuma",
        "Back" to "Emuva",
        "Open" to "Vula",
        "Show password" to "Bonisa iphasiwedi",
        "Hide password" to "Fihla iphasiwedi",
        "We need permission for the service you use\nLearn More" to "Sidinga imvume yesevisi oyisebenzisayo\nFunda kabanzi",
        "Learn More" to "Funda kabanzi",
        "Settings" to "Izilungiselelo",
        "Profile" to "Iphrofayela",
        "Language Option" to "Ulimi",
        "Language: English" to "Ulimi: isiNgisi",
        "Language: isiZulu" to "Ulimi: isiZulu",
        "Change Language" to "Shintsha ulimi",
        "Select Language" to "Khetha ulimi",
        "English" to "isiNgisi",
        "isiZulu" to "isiZulu",
        "Language changed" to "Ulimi lushintshiwe",
        "Bank Account" to "I-akhawunti yasebhange",
        "Bank Accounts" to "Ama-akhawunti asebhange",
        "Add bank account" to "Engeza i-akhawunti yasebhange",
        "Manage bank accounts" to "Phatha ama-akhawunti asebhange",
        "Pay from bank account" to "Khokha nge-akhawunti yasebhange",
        "Account name (e.g. Personal)" to "Igama le-akhawunti (isib. Eyami)",
        "Bank name" to "Igama lebhange",
        "Starting demo balance (R)" to "Ibhalansi yokuqala yokuzama (R)",
        "Use for seller payouts" to "Sebenzisa ukuthola izinkokhelo zokuthengisa",
        "Edit balance" to "Hlela ibhalansi",
        "Edit account balance" to "Hlela ibhalansi ye-akhawunti",
        "Delete bank account?" to "Susa i-akhawunti yasebhange?",
        "Current balance (R)" to "Ibhalansi yamanje (R)",
        "Enter a valid amount" to "Faka inani elivumelekile",
        "Balance cannot be negative" to "Ibhalansi ayikwazi ukuba ngaphansi kuka-zero",
        "Default seller payout account" to "I-akhawunti ezenzakalelayo yokuthola izinkokhelo",
        "Recent transactions" to "Imisebenzi yakamuva",
        "No transactions yet" to "Akukho misebenzi okwamanje",
        "Add up to three accounts to track demo payments." to "Engeza ama-akhawunti afinyelela kwamathathu ukuze ulandele izinkokhelo zokuzama.",
        "Maximum of 3 accounts reached" to "Usufinyelele umkhawulo wama-akhawunti angu-3",
        "Demo balances only. No real bank account numbers are stored and no real money is moved." to "Amabhalansi okuzama kuphela. Izinombolo zangempela zasebhange azigcinwa futhi ayikho imali yangempela ethunyelwayo.",
        "Demo mode: account balances update inside Vogue Cycle only. No real bank connection or charge is made." to "Indlela yokuzama: amabhalansi abuyekezwa ngaphakathi kwe-Vogue Cycle kuphela. Alukho uxhumano noma ukukhokhiswa kwemali yasebhange yangempela.",
        "No bank accounts saved — add one in Settings" to "Awekho ama-akhawunti asebhange agciniwe — engeza eyodwa kokuthi Izilungiselelo",
        "The selected demo account does not have enough balance" to "I-akhawunti yokuzama ekhethiwe ayinayo ibhalansi eyanele",
        "Add and choose a bank account first" to "Qala ngokwengeza bese ukhetha i-akhawunti yasebhange",
        "Contact Details" to "Imininingwane yokuxhumana",
        "Add contact detail" to "Engeza imininingwane yokuxhumana",
        "Phone number" to "Inombolo yocingo",
        "Enter a phone number or email address" to "Faka inombolo yocingo noma ikheli le-imeyili",
        "Enter a valid phone number or email address" to "Faka inombolo yocingo noma ikheli le-imeyili elivumelekile",
        "No contact details saved yet" to "Ayikho imininingwane yokuxhumana egciniwe okwamanje",
        "Add phone numbers and email addresses for your account. These details are private to you." to "Engeza izinombolo zocingo namakheli e-imeyili e-akhawunti yakho. Le mininingwane ibonwa nguwe kuphela.",
        "Remove" to "Susa",
        "Remove contact detail?" to "Susa imininingwane yokuxhumana?",
        "Save" to "Londoloza",
        "Cancel" to "Khansela",
        "Your privacy matters" to "Ubumfihlo bakho bubalulekile",
        "VogueCycle respects your privacy. This page explains how information is used when you browse, buy, sell, or communicate through the marketplace." to "I-VogueCycle iyabuhlonipha ubumfihlo bakho. Leli khasi lichaza indlela imininingwane esetshenziswa ngayo uma ubheka, uthenga, uthengisa noma uxhumana nabanye emakethe.",
        "Information we use" to "Imininingwane esiyisebenzisayo",
        "VogueCycle uses account and profile details, contact details you add, listing information and photos, messages, order and delivery details, reviews, and your app preferences to provide the marketplace." to "I-VogueCycle isebenzisa imininingwane ye-akhawunti nephrofayela, imininingwane yokuxhumana oyengezayo, imininingwane nezithombe zezinto ozifakayo, imilayezo, ama-oda nokulethwa, izibuyekezo, kanye nezinketho zakho zohlelo lokusebenza ukuze inikeze izinsiza zemakethe.",
        "How information is used" to "Indlela imininingwane esetshenziswa ngayo",
        "Information helps create and manage your account, display your profile and listings, support buyer and seller conversations, process orders and delivery, maintain platform safety, and respond to support requests." to "Imininingwane isiza ukudala nokuphatha i-akhawunti yakho, ukubonisa iphrofayela nezinto ozifakile, ukusekela izingxoxo zabathengi nabathengisi, ukuphatha ama-oda nokulethwa, ukugcina ukuphepha kohlelo, nokuphendula izicelo zosizo.",
        "What other users can see" to "Lokho abanye abasebenzisi abangakubona",
        "Your username, public profile information, listings, and reviews may be visible to other users. Contact details saved in Contact Details are private to your account. Order and delivery information is shared only with the people involved when needed to complete an order." to "Igama lakho lomsebenzisi, imininingwane yephrofayela ebonakalayo, izinto ozifakile nezibuyekezo kungabonwa abanye abasebenzisi. Imininingwane oyigcine kokuthi Imininingwane Yokuxhumana iyimfihlo ku-akhawunti yakho. Imininingwane ye-oda nokulethwa yabelwa abantu abathintekayo kuphela uma kudingeka ukuze kuqedwe i-oda.",
        "Your choices" to "Izinketho zakho",
        "You can review and update your profile and contact details in Settings. Before posting an item, choose the photos and description you want shoppers to see. Avoid including sensitive personal information in public listings or messages." to "Ungabuyekeza futhi ushintshe iphrofayela nemininingwane yokuxhumana kokuthi Izilungiselelo. Ngaphambi kokufaka into, khetha izithombe nencazelo ofuna abathengi bakubone. Gwema ukufaka imininingwane yomuntu ebucayi ezintweni ezibonwa umphakathi noma emilayezweni.",
        "Consent" to "Imvume",
        "VogueCycle uses personal information for purposes needed to provide the marketplace and, where required, with your consent or another lawful justification. You decide what profile and listing information to make public." to "I-VogueCycle isebenzisa imininingwane yomuntu ngezinhloso ezidingekayo ukuze inikeze imakethe, futhi lapho kudingeka, ngemvume yakho noma ngesinye isizathu esisemthethweni. Nguwe onquma ukuthi yimiphi imininingwane yephrofayela nezinto ozifakile ebonakala emphakathini.",
        "Data care" to "Ukuvikelwa kwemininingwane",
        "Account information is stored with the services used to operate VogueCycle and is protected by account access controls. Information may be retained while your account is active and when needed to maintain order records, protect users, or meet applicable requirements." to "Imininingwane ye-akhawunti igcinwa kumasevisi asetshenziswa yi-VogueCycle futhi ivikelwa yizilawuli zokungena ku-akhawunti. Imininingwane ingagcinwa ngesikhathi i-akhawunti yakho isasebenza nalapho kudingeka khona ukugcina amarekhodi ama-oda, ukuvikela abasebenzisi noma ukuhlangabezana nezimfuneko ezifanele.",
        "Your privacy rights" to "Amalungelo akho obumfihlo",
        "You may ask to access or correct your personal information, request deletion where applicable, or object to certain processing. You may also raise a privacy complaint with the Information Regulator of South Africa." to "Ungacela ukufinyelela noma ukulungisa imininingwane yakho, ucele isuswe lapho kuvumelekile, noma uphikisane nokusetshenziswa kwayo okuthile. Ungafaka nesikhalazo sobumfihlo kuMlawuli Wolwazi waseNingizimu Afrika.",
        "Questions or updates" to "Imibuzo noma izibuyekezo",
        "Keep your contact details current so you can receive important account and order communications. For an account or privacy request, contact VogueCycle through its official support channel." to "Gcina imininingwane yakho yokuxhumana isesikhathini ukuze uthole imilayezo ebalulekile nge-akhawunti nama-oda. Ngesicelo se-akhawunti noma sobumfihlo, xhumana ne-VogueCycle usebenzisa indlela yayo esemthethweni yokweseka abasebenzisi.",
        "About VogueCycle" to "Mayelana ne-VogueCycle",
        "VogueCycle is a marketplace for buying and selling pre-loved and vintage clothing. It brings shoppers and independent sellers together to give great fashion pieces another life." to "I-VogueCycle iyimakethe yokuthenga nokuthengisa izingubo esezike zathandwa kanye neze-vintage. Ihlanganisa abathengi nabathengisi abazimele ukuze inike izingubo ezinhle ithuba lokuphinda zigqokwe.",
        "Find your next favourite" to "Thola into ozoyithanda elandelayo",
        "Browse clothing listings, compare item details, and message sellers when you need more information. Check the listed condition, sizing, price, and delivery details before placing an order." to "Bheka izingubo ezifakiwe, uqhathanise imininingwane yazo, bese uthumela umthengisi umlayezo uma udinga eminye imininingwane. Hlola isimo, usayizi, intengo nemininingwane yokulethwa ngaphambi kokufaka i-oda.",
        "Sell on VogueCycle" to "Thengisa ku-VogueCycle",
        "Create a shop by listing items with clear photos, accurate descriptions, honest condition details, and a fair price. Respond to buyer questions and keep order and delivery information up to date." to "Dala isitolo ngokufaka izinto ezinezithombe ezicacile, izincazelo eziqondile, imininingwane ethembekile ngesimo sazo nentengo efanele. Phendula imibuzo yabathengi futhi ugcine imininingwane yama-oda nokulethwa isesikhathini.",
        "Our community" to "Umphakathi wethu",
        "We want fashion to stay in motion through thoughtful reuse and respectful trading. Please describe items accurately, communicate clearly, protect personal information, and treat other members with respect." to "Sifuna imfashini iqhubeke ngokuphinda kusetshenziswe izingubo nangokuhwebelana ngenhlonipho. Chaza izinto ngokunembile, xhumana ngokucacile, vikela imininingwane yomuntu futhi uphathe amanye amalungu ngenhlonipho.",
        "Marketplace experience" to "Indlela imakethe esebenza ngayo",
        "VogueCycle provides tools for browsing, listing, messaging, orders, delivery updates, and reviews. Buyers and sellers should use the information in each listing and order to make informed decisions." to "I-VogueCycle inikeza amathuluzi okubheka nokufaka izinto, ukuthumela imilayezo, ukuphatha ama-oda, ukubona izibuyekezo zokulethwa nokubhala izibuyekezo. Abathengi nabathengisi kufanele basebenzise imininingwane yento ne-oda ukuze benze izinqumo ezinolwazi.",
        "Notifications" to "Izaziso",
        "Mark all read" to "Maka zonke njengezifundiwe",
        "No notifications yet" to "Azikho izaziso okwamanje",
        "Live updates are on" to "Izibuyekezo ezibukhoma zivuliwe",
        "Live updates unavailable. Check your connection." to "Izibuyekezo ezibukhoma azitholakali. Hlola uxhumano lwakho.",
        "New order" to "I-oda elisha",
        "Order update" to "Isibuyekezo se-oda",
        "New message" to "Umlayezo omusha",
        "Could not mark notifications as read. Please try again." to "Akukwazanga ukumaka izaziso njengezifundiwe. Zama futhi.",
        "Privacy & Consent" to "Ubumfihlo nemvume",
        "Shop VougeCycle Pty Ltd" to "Isitolo i-VougeCycle Pty Ltd",
        "Home" to "Ikhaya",
        "Explore" to "Hlola",
        "Sell" to "Thengisa",
        "Inbox" to "Imilayezo",
        "My shop" to "Isitolo sami",
        "My Shop" to "Isitolo sami",
        "Shop" to "Isitolo",
        "Search" to "Sesha",
        "Search items and shops" to "Sesha izinto nezitolo",
        "Search products" to "Sesha imikhiqizo",
        "Discover" to "Thola",
        "Purchases" to "Okuthengiwe",
        "Favorites" to "Izintandokazi",
        "MY SHOP" to "ISITOLO SAMI",
        "More Options" to "Ezinye izinketho",
        "Add conditions" to "Engeza izimo",
        "+Add links" to "+Engeza izixhumanisi",
        "Add description" to "Engeza incazelo",
        "Paste a website or social link" to "Faka isixhumanisi sewebhusayithi noma senkundla yokuxhumana",
        "Add website or social links, one per line." to "Faka izixhumanisi zewebhusayithi noma zenkundla yokuxhumana, esisodwa emugqeni ngamunye.",
        "Enter valid website links" to "Faka izixhumanisi zewebhusayithi ezivumelekile",
        "Shop links saved" to "Izixhumanisi zesitolo zilondoloziwe",
        "Favourites" to "Izintandokazi",
        "Favourite" to "Intandokazi",
        "Orders" to "Ama-oda",
        "My purchases" to "Engikuthengile",
        "View my purchases" to "Buka engikuthengile",
        "Sell an item" to "Thengisa into",
        "List Item" to "Faka into ohlwini",
        "Title" to "Isihloko",
        "Description" to "Incazelo",
        "Describe your item..." to "Chaza into yakho...",
        "Description goes here..." to "Incazelo ibhalwa lapha...",
        "Price (R)" to "Intengo (R)",
        "e.g. 1700.00" to "isib. 1700.00",
        "Category" to "Isigaba",
        "Choose category" to "Khetha isigaba",
        "Condition" to "Isimo",
        "Choose item condition" to "Khetha isimo sento",
        "Item details" to "Imininingwane yento",
        "Images" to "Izithombe",
        "Product image" to "Isithombe somkhiqizo",
        "Product photo" to "Isithombe somkhiqizo",
        "Upload photo" to "Layisha isithombe",
        "Upload" to "Layisha",
        "Change photo" to "Shintsha isithombe",
        "Add one product image" to "Faka isithombe esisodwa somkhiqizo",
        "Remember to use photos you've taken yourself. Images taken from the internet are not allowed on VougeCycle and violate our T&Cs." to "Sebenzisa izithombe ozithathele zona. Izithombe ezithathwe ku-inthanethi azivunyelwe ku-VougeCycle futhi zephula imigomo nemibandela yethu.",
        "Location" to "Indawo",
        "Address" to "Ikheli",
        "Town, province" to "Idolobha, isifundazwe",
        "Street, suburb, city and postal code" to "Umgwaqo, indawo yokuhlala, idolobha nekhodi yeposi",
        "Phone number e.g. 0821234567" to "Inombolo yocingo isib. 0821234567",
        "Seller" to "Umthengisi",
        "Seller reviews" to "Izibuyekezo zomthengisi",
        "Review seller" to "Bhala isibuyekezo ngomthengisi",
        "Your rating" to "Isilinganiso sakho",
        "Your review" to "Isibuyekezo sakho",
        "Describe your experience with the seller" to "Chaza ulwazi lwakho ngomthengisi",
        "Review comment" to "Amazwana esibuyekezo",
        "Submit review" to "Thumela isibuyekezo",
        "Delete review" to "Susa isibuyekezo",
        "No reviews yet" to "Azikho izibuyekezo okwamanje",
        "Completed buyers can review this seller" to "Abathengi abaqedile ukuthenga bangabhala isibuyekezo ngalo mthengisi",
        "Save profile" to "Londoloza iphrofayela",
        "Edit profile" to "Hlela iphrofayela",
        "Update how your shop appears to other users" to "Shintsha indlela isitolo sakho esibonakala ngayo kwabanye abasebenzisi",
        "Add description/slogan" to "Faka incazelo noma isiqubulo",
        "Tell buyers about your shop" to "Tshela abathengi ngesitolo sakho",
        "Add links ...." to "Faka izixhumanisi ....",
        "Active today" to "Isebenza namuhla",
        "1 sold" to "Kuthengiswe oku-1",
        "New Arrivals" to "Okusha okufikile",
        "Brands" to "Imikhiqizo",
        "Women" to "Abesifazane",
        "Men" to "Amadoda",
        "Eras" to "Izikhathi",
        "Vintage" to "Okwakudala",
        "Sports" to "Ezemidlalo",
        "All" to "Konke",
        "All conditions" to "Zonke izimo",
        "Include sold items" to "Faka nezinto esezithengisiwe",
        "Min price" to "Intengo ephansi",
        "Max price" to "Intengo ephezulu",
        "Clear filters" to "Sula izihlungi",
        "Newest" to "Okusha kakhulu",
        "Price: Low to high" to "Intengo: kusuka kokuphansi kuya kokuphezulu",
        "Price: High to low" to "Intengo: kusuka kokuphezulu kuya kokuphansi",
        "No products available yet" to "Ayikho imikhiqizo okwamanje",
        "No products found" to "Ayikho imikhiqizo etholakele",
        "Try changing or clearing your filters" to "Zama ukushintsha noma ukusula izihlungi zakho",
        "No messages yet" to "Ayikho imilayezo okwamanje",
        "Start a conversation by messaging a seller" to "Qala ingxoxo ngokuthumela umthengisi umlayezo",
        "Type a message" to "Bhala umlayezo",
        "Message" to "Umlayezo",
        "Send" to "Thumela",
        "No conversation selected" to "Ayikho ingxoxo ekhethiwe",
        "My shop" to "Isitolo sami",
        "Shop orders" to "Ama-oda esitolo",
        "Your shop has no orders yet" to "Isitolo sakho asikabi nawo ama-oda",
        "Orders" to "Ama-oda",
        "Order" to "I-oda",
        "Track order" to "Landela i-oda",
        "Track your order" to "Landela i-oda lakho",
        "Tracking number" to "Inombolo yokulandelela",
        "Delivery address" to "Ikheli lokulethwa",
        "Delivery details" to "Imininingwane yokulethwa",
        "Delivery method" to "Indlela yokuletha",
        "Delivery" to "Ukulethwa",
        "Delivery options" to "Izindlela zokuletha",
        "Both courier options" to "Zombili izindlela zokuthumela",
        "Door-to-door only" to "Ukulethwa emnyango kuphela",
        "Locker collection only" to "Ukuqoqwa ekhabetheni lokuthumela kuphela",
        "Delivery options buyers can choose" to "Izindlela zokulethwa abathengi abangazikhetha",
        "Select courier and method" to "Khetha inkampani yokuthumela nendlela yokuletha",
        "Select courier and method" to "Khetha inkampani yokuthumela nendlela yokuletha",
        "Choose a courier and delivery method" to "Khetha inkampani yokuthumela nendlela yokuletha",
        "Choose courier" to "Khetha inkampani yokuthumela",
        "Choose delivery method" to "Khetha indlela yokuletha",
        "Courier" to "Inkampani yokuthumela",
        "Courier • Delivery method" to "Inkampani yokuthumela • Indlela yokuletha",
        "Delivery progress" to "Ukuqhubeka kokulethwa",
        "Preparing parcel" to "Kulungiswa iphasela",
        "Out for delivery" to "Lisendleleni yokulethwa",
        "Delivered" to "Lilethiwe",
        "Shipped" to "Lithunyelwe",
        "Paid" to "Kukhokhiwe",
        "PENDING" to "KULINDELE",
        "Preparing parcel" to "Kulungiswa iphasela",
        "Dispatch parcel" to "Thumela iphasela",
        "Prepare parcel" to "Lungisa iphasela",
        "Mark paid" to "Maka njengokukhokhiwe",
        "Mark delivered" to "Maka njengokulethiwe",
        "Purchases" to "Okuthengiwe",
        "Purchased item" to "Into ethengiwe",
        "Ordered product" to "Umkhiqizo o-odiwe",
        "Recipient full name" to "Igama eligcwele lomamukeli",
        "Payment method" to "Indlela yokukhokha",
        "Choose a payment method" to "Khetha indlela yokukhokha",
        "Pay securely (Demo)" to "Khokha ngokuphephile (ukuzama)",
        "South African demo payment" to "Indlela yokukhokha yokuzama yaseNingizimu Afrika",
        "Instant EFT (Demo)" to "I-EFT esheshayo (ukuzama)",
        "PayShap (Demo)" to "PayShap (ukuzama)",
        "Card (Demo)" to "Ikhadi (ukuzama)",
        "Demo mode: no banking details are requested and no real money will be charged." to "Indlela yokuzama: akukho mininingwane yasebhange ecelwayo futhi akukho mali yangempela ezokhokhiswa.",
        "Payment successful" to "Ukukhokha kuphumelele",
        "Your demo order has been paid" to "I-oda lakho lokuzama selikhokhiwe",
        "Total: R 0.00" to "Isamba: R 0.00",
        "R 0.00" to "R 0.00",
        "Checkout" to "Qedela ukuthenga",
        "Buy now" to "Thenga manje",
        "Product title" to "Isihloko somkhiqizo",
        "Profile photo" to "Isithombe sephrofayela",
        "Avatar" to "Isithombe sephrofayela",
        "Empty" to "Akukho lutho",
        "Nothing here yet" to "Akukho lutho lapha okwamanje",
        "No reviews yet" to "Azikho izibuyekezo okwamanje",
        "You have no favourite items yet" to "Awukabi nazo izinto ozithandayo",
        "You have not purchased anything yet" to "Akukabikho lutho osuluthengile",
        "Last message..." to "Umlayezo wokugcina...",
        "now" to "manje",
        "Maximum 160 characters" to "Izinhlamvu ezingafika kwezingu-160",
        "Verified purchase • Maximum 500 characters" to "Ukuthenga okuqinisekisiwe • Izinhlamvu ezingafika kwezingu-500",
        "e.g. Vintage CDG Shirt" to "isib. Ihembe le-CDG lakudala",
        "Additional Details" to "Imininingwane eyengeziwe",
        "Size" to "Usayizi",
        "Colour" to "Umbala",
        "Material" to "Impahla",
        "Brand (optional)" to "Uhlobo (akudingeki)",
        "Size (optional)" to "Usayizi (akudingeki)",
        "Colour (optional)" to "Umbala (akudingeki)",
        "Material (optional)" to "Impahla (akudingeki)",
        "Delivery notes for buyers (optional)" to "Amanothi okulethwa abathengi (akudingeki)",
        "› Additional Details" to "› Imininingwane eyengeziwe",
        "› Delivery" to "› Ukulethwa",
        "Privacy & Consent" to "Ubumfihlo nemvume",
        "Session stays signed in" to "Uzohlala ungene ngemvume",
        "Forgot Password" to "Ukhohlwe iphasiwedi",
        "Forgot Password?" to "Ukhohlwe iphasiwedi?",
        "Enter your email address and we'll send you a password reset link." to "Faka ikheli lakho le-imeyili sizokuthumelela isixhumanisi sokusetha kabusha iphasiwedi.",
        "Retry" to "Zama futhi",
        "Retry loading" to "Zama ukulayisha futhi",
        "Preview" to "Buka kuqala",
        "Location not provided" to "Indawo ayikafakwa",
        "Address unavailable" to "Ikheli alitholakali",
        "Delivery method unavailable" to "Indlela yokuletha ayitholakali",
        "Delivery estimate unavailable" to "Isikhathi sokulethwa asitholakali",
        "Estimated delivery will appear here" to "Isikhathi sokulethwa sizovela lapha",
        "Generated when the seller dispatches your parcel" to "Kwenziwa lapho umthengisi ethumela iphasela lakho",
        "Your review" to "Isibuyekezo sakho",
        "No products available yet" to "Ayikho imikhiqizo okwamanje",
        "No products found" to "Ayikho imikhiqizo etholakele",
        "1" to "1",
        "1 sold" to "Kuthengiswe oku-1",
        "SOLD" to "THENGISIWE",
        "Product" to "Umkhiqizo",
        "Category" to "Isigaba",
        "Condition" to "Isimo",
        "Price (R)" to "Intengo (R)",
        "List Item" to "Faka into ohlwini",
        "Log out" to "Phuma",
        "Cancel" to "Khansela",
        "Delete" to "Susa",
        "Save profile" to "Londoloza iphrofayela",
        "Update review" to "Buyekeza isibuyekezo",
        "Add items" to "Faka izinto",
        "View shop" to "Buka isitolo",
        "Bought" to "Kuthengiwe",
        "Review" to "Isibuyekezo",
        "Search products" to "Sesha imikhiqizo",
        "Brand" to "Uhlobo",
        "Estimate" to "Isilinganiso",
        "Estimated delivery: 2–3 working days" to "Kulindeleke ukuthi kufike ezinsukwini zokusebenza ezi-2–3",
        "Estimated delivery: 2–4 working days" to "Kulindeleke ukuthi kufike ezinsukwini zokusebenza ezi-2–4",
        "Estimated delivery: 3–5 working days" to "Kulindeleke ukuthi kufike ezinsukwini zokusebenza ezi-3–5",
        "Don't have an account?" to "Awunayo i-akhawunti?",
        "Username, Email or Phone Number" to "Igama lomsebenzisi, i-imeyili noma inombolo yocingo",
        "username" to "igama lomsebenzisi",
        "Gauteng, Braamfontein" to "Gauteng, eBraamfontein",
        "Shipping" to "Ukuthumela",
        "Shop bio" to "Incazelo yesitolo",
        "No products found" to "Ayikho imikhiqizo etholakele",
        "Try changing or clearing your filters" to "Zama ukushintsha noma ukusula izihlungi zakho",
        "Completed buyers can review this seller" to "Abathengi abaqedile bangabhala isibuyekezo ngalo mthengisi",
        "Buy now" to "Thenga manje",
        "Enter a valid South African phone number" to "Faka inombolo yocingo yaseNingizimu Afrika esebenzayo",
        "Enter the full delivery address" to "Faka ikheli eligcwele lokulethwa",
        "Enter the recipient's name" to "Faka igama lomamukeli",
        "Please fill in all fields" to "Gcwalisa zonke izikhala",
        "Please enter a valid email" to "Faka i-imeyili esebenzayo",
        "Please enter a valid email address" to "Faka ikheli le-imeyili elisebenzayo",
        "Please enter a title" to "Faka isihloko",
        "Please enter a price" to "Faka intengo",
        "Please enter a valid price" to "Faka intengo esebenzayo",
        "Please add a description" to "Faka incazelo",
        "Please choose a category" to "Khetha isigaba",
        "Please choose a condition" to "Khetha isimo",
        "Please choose 1 to 5 stars" to "Khetha inkanyezi eyodwa kuya kweziyisihlanu",
        "Write between 3 and 500 characters" to "Bhala izinhlamvu eziphakathi kwezingu-3 nezingu-500",
        "Passwords do not match" to "Amaphasiwedi awafani",
        "Password must be at least 6 characters" to "Iphasiwedi kufanele okungenani ibe nezinhlamvu eziyisi-6",
        "Password must contain a capital letter" to "Iphasiwedi kufanele ibe nohlamvu olulodwa olukhulu",
        "Password must contain a number" to "Iphasiwedi kufanele ibe nenombolo",
        "Use 3–30 letters, numbers or underscores" to "Sebenzisa izinhlamvu, izinombolo noma amadeshi angaphansi kuka-3 kuya ku-30",
        "The request took too long. Please try again." to "Isicelo sithathe isikhathi eside kakhulu. Zama futhi.",
        "No internet connection. Check your connection and try again." to "Ayikho i-inthanethi. Hlola uxhumano lwakho bese uzama futhi.",
        "Your session has expired. Please log in again." to "Isikhathi sakho sesiphelile. Ngena ngemvume futhi.",
        "That information already exists." to "Lolo lwazi selukhona.",
        "You do not have permission to complete this action." to "Awunayo imvume yokwenza lokhu.",
        "Could not complete this action. Please try again." to "Akukwazanga ukuqedela lokhu. Zama futhi.",
        "Could not update favourite" to "Akukwazanga ukubuyekeza intandokazi",
        "Added to favourites" to "Kwengezwe ezintandokazini",
        "Removed from favourites" to "Kususwe ezintandokazini",
        "Could not open the conversation" to "Akukwazanga ukuvula ingxoxo",
        "Could not read selected image" to "Akukwazanga ukufunda isithombe esikhethiwe",
        "Could not read selected photo" to "Akukwazanga ukufunda isithombe esikhethiwe",
        "No reviews yet" to "Azikho izibuyekezo okwamanje",
        "Review saved" to "Isibuyekezo silondoloziwe",
        "Review deleted" to "Isibuyekezo sisusiwe",
        "Are you sure you want to delete this review?" to "Uqinisekile ukuthi ufuna ukususa lesi sibuyekezo?",
        "Are you sure you want to log out?" to "Uqinisekile ukuthi ufuna ukuphuma?",
        "Log out failed. Please try again." to "Ukuphuma kuhlulekile. Zama futhi.",
        "Bank account coming soon" to "Imininingwane yasebhange izotholakala maduze",
        "Contact details coming soon" to "Imininingwane yokuxhumana izotholakala maduze",
        "Notification settings coming soon" to "Izilungiselelo zezaziso zizotholakala maduze",
        "Privacy & consent coming soon" to "Ubumfihlo nemvume kuzotholakala maduze",
        "Additional Details coming soon" to "Imininingwane eyengeziwe izotholakala maduze",
        "Delivery options coming soon" to "Izindlela zokuletha zizotholakala maduze",
        "Add links coming soon" to "Ukufaka izixhumanisi kuzotholakala maduze",
        "Conversation search will be added later" to "Ukusesha ezingxoxweni kuzongezwa kamuva",
        "Please wait for the photo upload to finish" to "Linda kuze kuqedwe ukulayishwa kwesithombe",
        "You must be logged in" to "Kufanele ungene ngemvume",
        "You cannot message yourself" to "Awukwazi ukuzithumelela umlayezo",
        "You cannot buy your own item" to "Awukwazi ukuthenga into yakho",
        "This item has already been sold" to "Le nto isithengisiwe",
        "Message cannot be empty" to "Umlayezo awukwazi ukungabi nalutho",
        "Message is too long" to "Umlayezo mude kakhulu",
        "Please choose a payment method" to "Khetha indlela yokukhokha",
        "Processing demo payment..." to "Kucutshungulwa inkokhelo yokuzama...",
        "Creating account..." to "Kwenziwa i-akhawunti...",
        "Logging in..." to "Kungenwa ngemvume...",
        "Sending..." to "Kuyathunyelwa...",
        "Saving..." to "Kuyalondolozwa...",
        "Listing..." to "Kufakwa ohlwini...",
        "Uploading..." to "Kuyalayishwa...",
        "Welcome back!" to "Siyakwamukela futhi!",
        "Password reset email sent" to "I-imeyili yokusetha kabusha iphasiwedi ithunyelwe",
        "Profile updated" to "Iphrofayela ibuyekeziwe",
        "Item listed successfully!" to "Into ifakwe ohlwini ngempumelelo!",
        "Incorrect email or password." to "I-imeyili noma iphasiwedi ayilungile.",
        "No description provided." to "Ayikho incazelo efakiwe.",
        "This order is already delivered" to "Le oda isilethiwe",
        "The order must be delivered before it can be reviewed" to "I-oda kufanele ilethwe ngaphambi kokuba kubhalwe isibuyekezo",
        "You cannot access this conversation" to "Awukwazi ukufinyelela le ngxoxo",
        "You cannot buy your own item" to "Awukwazi ukuthenga into yakho",
        "You cannot message yourself" to "Awukwazi ukuzithumelela umlayezo",
        "You have no favourite items yet" to "Awukabi nazo izinto ozithandayo",
        "You have not purchased anything yet" to "Akukabikho lutho osuluthengile",
        "Your shop has no orders yet" to "Isitolo sakho asikabi nawo ama-oda",
        "Failed to load seller profile" to "Iphrofayela yomthengisi ayikwazanga ukulayishwa",
        "Failed to load shop" to "Isitolo asikwazanga ukulayishwa",
        "Offline — showing recently loaded products" to "Awuxhunyiwe ku-inthanethi — kuboniswa imikhiqizo esanda kulayishwa",
        "Choose a rating from 1 to 5 stars" to "Khetha isilinganiso senkanyezi eyodwa kuya kweziyisihlanu",
        "Mark paid" to "Maka njengokukhokhiwe",
        "Mark delivered" to "Maka njengokulethiwe",
        "Choose a courier and delivery method" to "Khetha inkampani yokuthumela nendlela yokuletha",
        "Estimated delivery will appear here" to "Isikhathi sokulethwa sizovela lapha",
        "Courier not selected" to "Akukakhethwa inkampani yokuthumela",
        "Address unavailable" to "Ikheli alitholakali",
        "Delivery estimate unavailable" to "Isikhathi sokulethwa asitholakali",
        "Delivery method unavailable" to "Indlela yokuletha ayitholakali",
        "Image must be smaller than 10 MB" to "Isithombe kufanele sibe ngaphansi kuka-10 MB",
        "Profile photo must be smaller than 5 MB" to "Isithombe sephrofayela kufanele sibe ngaphansi kuka-5 MB",
        "No messages yet" to "Ayikho imilayezo okwamanje",
        "Start the conversation" to "Qala ingxoxo",
        "Message" to "Umlayezo",
        "Phone number e.g. 0821234567" to "Inombolo yocingo isib. 0821234567",
        "Select Language" to "Khetha ulimi",
        "Cancel" to "Khansela",
        "Language Option" to "Ukukhetha ulimi",
        "The language change will be saved on this phone." to "Ushintsho lolimi luzogcinwa kule foni.",
        "We need permission for the service you use" to "Sidinga imvume yesevisi oyisebenzisayo",
        "Learn More" to "Funda kabanzi",
        "+ Add items" to "+ Faka izinto",
        "New with tags" to "Kusha okunamathegi",
        "New without tags" to "Kusha okungenamathegi",
        "Used - Excellent" to "Kusetshenzisiwe - Kuhle kakhulu",
        "Used - Good" to "Kusetshenzisiwe - Kuhle",
        "Used - Fair" to "Kusetshenzisiwe - Kulungile",
        "Door-to-door" to "Kusuka emnyango kuye emnyango",
        "Locker collection" to "Ukuqoqa ekhabetheni lokugcina",
        "Instant EFT" to "I-EFT esheshayo",
        "Card" to "Ikhadi",
        "Buyer" to "Umthengi",
        "Chat" to "Ingxoxo",
        "Product unavailable" to "Umkhiqizo awutholakali",
        "Only the buyer can review this order" to "Umthengi kuphela ongabhala isibuyekezo sale oda",
        "Review must contain 3 to 500 characters" to "Isibuyekezo kufanele sibe nezinhlamvu ezingu-3 kuya kwezingu-500",
        "Bio must be 160 characters or less" to "Incazelo kufanele ibe nezinhlamvu ezingu-160 noma ngaphansi",
        "Enter your full name" to "Faka igama lakho eligcwele",
        "Enter your town or province" to "Faka idolobha noma isifundazwe sakho",
        "That username is already taken" to "Lelo gama lomsebenzisi selisetshenziswa",
        "Photo uploaded" to "Isithombe silayishiwe",
        "Notifications coming soon" to "Izaziso zizotholakala maduze",
        "Could not open the conversation" to "Akukwazanga ukuvula ingxoxo",
        "Could not load shop" to "Akukwazanga ukulayisha isitolo",
        "Could not load profile" to "Akukwazanga ukulayisha iphrofayela",
        "Could not load purchases" to "Akukwazanga ukulayisha okuthengiwe",
        "Could not load messages" to "Akukwazanga ukulayisha imilayezo",
        "Could not load products" to "Akukwazanga ukulayisha imikhiqizo",
        "Could not load favourites" to "Akukwazanga ukulayisha izintandokazi",
        "Could not save profile" to "Akukwazanga ukulondoloza iphrofayela",
        "Could not save review" to "Akukwazanga ukulondoloza isibuyekezo",
        "Could not complete payment" to "Akukwazanga ukuqedela inkokhelo",
        "Could not log in" to "Akukwazanga ukungena ngemvume",
        "Could not update order" to "Akukwazanga ukubuyekeza i-oda",
        "Could not send message" to "Akukwazanga ukuthumela umlayezo",
        "Could not delete review" to "Akukwazanga ukususa isibuyekezo"
    )

    private val zuluToEnglish = englishToZulu.entries.associate { (english, zulu) -> zulu to english }

    fun isZulu(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LANGUAGE, "en") == "zu"

    fun translate(context: Context, value: String): String {
        val source = value.trim()
        val leading = value.takeWhile { it.isWhitespace() }
        val trailing = value.takeLastWhile { it.isWhitespace() }
        val translated = if (isZulu(context)) {
            val statusPrefix = when {
                source.startsWith("✓  ") -> "✓  "
                source.startsWith("○  ") -> "○  "
                else -> null
            }
            if (statusPrefix != null) {
                val status = source.removePrefix(statusPrefix)
                statusPrefix + (englishToZulu[status] ?: translateDynamic(status))
            } else englishToZulu[source] ?: if (source.contains('\n')) {
                source.split('\n').joinToString("\n") { englishToZulu[it] ?: translateDynamic(it) }
            } else {
                translateDynamic(source)
            }
        } else {
            zuluToEnglish[source] ?: if (source.contains('\n')) {
                source.split('\n').joinToString("\n") { zuluToEnglish[it] ?: it }
            } else {
                source
            }
        }
        return leading + translated + trailing
    }

    private fun translateDynamic(source: String): String {
        val dynamicPhrases = listOf(
            "Total: " to "Isamba: ",
            "Shipping: " to "Ukuthumela: ",
            "Balance: " to "Ibhalansi: ",
            "Opening balance: " to "Ibhalansi yokuqala: ",
            "Order marked " to "I-oda limakwe ngokuthi ",
            "Could not load reviews: " to "Ayikwazanga ukulayisha izibuyekezo: ",
            "Could not open chat: " to "Ayikwazanga ukuvula ingxoxo: ",
            "Could not send reset email: " to "Ayikwazanga ukuthumela i-imeyili yokusetha kabusha: ",
            "Listing failed: " to "Ukufaka ohlwini kuhlulekile: ",
            "Sign up failed: " to "Ukubhalisa kuhlulekile: ",
            "Upload failed: " to "Ukulayisha kuhlulekile: ",
            "Account created! Welcome, " to "I-akhawunti idaliwe! Siyakwamukela, ",
            "Account created. Check your email to confirm it." to "I-akhawunti idaliwe. Hlola i-imeyili yakho ukuze uyiqinisekise.",
            "Total " to "Isamba "
        )
        for ((english, zulu) in dynamicPhrases) {
            if (source.startsWith(english)) {
                val suffix = source.removePrefix(english)
                if (english == "Order marked ") {
                    val status = mapOf(
                        "pending" to "kulindile",
                        "paid" to "kukhokhiwe",
                        "shipped" to "kuthunyelwe",
                        "out for delivery" to "kusekulethweni",
                        "delivered" to "kulethiwe"
                    )[suffix] ?: suffix
                    return zulu + status
                }
                return zulu + suffix
            }
        }
        if (source.startsWith("Could not ") && source.endsWith(". Please try again.")) {
            val action = source.removePrefix("Could not ").removeSuffix(". Please try again.")
            val translatedAction = mapOf(
                "load profile" to "ukulayisha iphrofayela",
                "load shop" to "ukulayisha isitolo",
                "load products" to "ukulayisha imikhiqizo",
                "load purchases" to "ukulayisha okuthengiwe",
                "load favourites" to "ukulayisha izintandokazi",
                "load messages" to "ukulayisha imilayezo",
                "load contact details" to "ukulayisha imininingwane yokuxhumana",
                "save contact details" to "ukulondoloza imininingwane yokuxhumana",
                "remove contact detail" to "ukususa imininingwane yokuxhumana",
                "load notifications" to "ukulayisha izaziso",
                "load review" to "ukulayisha isibuyekezo",
                "load shop orders" to "ukulayisha ama-oda esitolo",
                "save profile" to "ukulondoloza iphrofayela",
                "save review" to "ukulondoloza isibuyekezo",
                "complete payment" to "ukuqedela inkokhelo",
                "log in" to "ukungena ngemvume",
                "update favourite" to "ukubuyekeza intandokazi",
                "update order" to "ukubuyekeza i-oda",
                "send message" to "ukuthumela umlayezo",
                "delete review" to "ukususa isibuyekezo"
            )[action] ?: action
            return "Akukwazanga $translatedAction. Zama futhi."
        }
        if (source.startsWith("You do not have permission to ")) {
            return "Awunayo imvume yokwenza lokhu."
        }
        Regex("^(\\d+) items?$").matchEntire(source)?.let {
            return "Izinto ezitholakele: ${it.groupValues[1]}"
        }
        Regex("^(\\d+) sold$").matchEntire(source)?.let {
            return "Kuthengiswe izinto ezingu-${it.groupValues[1]}"
        }
        Regex("^★ ([0-9.]+) \\((\\d+) reviews?\\)$").matchEntire(source)?.let {
            return "★ ${it.groupValues[1]} (izibuyekezo ezi-${it.groupValues[2]})"
        }
        Regex("^★ ([0-9.]+) from (\\d+) reviews?$").matchEntire(source)?.let {
            return "★ ${it.groupValues[1]} kuzibuyekezo ezi-${it.groupValues[2]}"
        }
        return source
    }

    fun translateViewTree(view: View) {
        val context = view.context
        if (view.tag != USER_CONTENT_TAG) {
            if (view is EditText) {
                val hint = view.hint?.toString()
                val translatedHint = hint?.let { translate(context, it) }
                if (hint != translatedHint && translatedHint != null) view.hint = translatedHint
                val error = view.error?.toString()
                val translatedError = error?.let { translate(context, it) }
                if (error != translatedError && translatedError != null) view.error = translatedError
            } else if (view is TextView) {
                val currentText = view.text.toString()
                val translatedText = translate(context, currentText)
                if (currentText != translatedText) view.text = translatedText
            }
            val description = view.contentDescription?.toString()
            val translatedDescription = description?.let { translate(context, it) }
            if (description != translatedDescription && translatedDescription != null) {
                view.contentDescription = translatedDescription
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) translateViewTree(view.getChildAt(index))
        }
    }

    fun makeToast(context: Context, message: CharSequence, duration: Int): Toast =
        Toast.makeText(context, translate(context, message.toString()), duration)

    fun showLanguagePicker(activity: AppCompatActivity) {
        val options = if (isZulu(activity)) {
            arrayOf("isiNgisi", "isiZulu")
        } else {
            arrayOf("English", "isiZulu")
        }
        val current = if (isZulu(activity)) 1 else 0
        AlertDialog.Builder(activity)
            .setTitle(translate(activity, "Select Language"))
            .setSingleChoiceItems(options, current) { dialog, which ->
                val selected = if (which == 1) "zu" else "en"
                activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(LANGUAGE, selected)
                    .apply()
                dialog.dismiss()
                activity.recreate()
            }
            .setNegativeButton(translate(activity, "Cancel"), null)
            .show()
    }
}
