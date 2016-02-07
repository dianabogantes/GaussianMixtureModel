diagm = diag([1 2 3]);

load gmm_data.txt
%load datasmall.csv
%load data2D_40.csv
%x_data = datasmall(:,1);
%y_data = datasmall(:,2);
%x_data = data2D_40(:,1);
%y_data = data2D_40(:,2);
x_data = gmm_data(:,1);
y_data = gmm_data(:,2);

figure
plot(x_data,y_data,'.k');
% 
% 
weight1 = 0.480423;
mu1 = [0.07217338547731349,0.016669805075674532];
sigma1 = [4.776320873626651 1.8744559462009966; 1.8744559462009966 0.9140443825142588];
%mu1 = [5.587364380336136 2.136557071120024];
%mu1 = [6.0238886107086715,4.427776445617923];
%sigma1 = [15.272554408687046  -3.227969393430191; -3.227969393430191  3.3177797436305756 ];
%sigma1 = [4.212883974924335 -0.5835040140768615; -0.5835040140768615 6.3361440222298775];
mu2 = [-0.10458112516978531,0.042896010128645426];
sigma2 = [4.910348976716073 -2.0085289129783312; -2.0085289129783312 1.012105136248555];
weight2 = 0.519577;
%mu2 = [2.9340735015486428 4.92323629663431];
%mu2 = [8.895364692569393,3.8397010023855787];
%sigma2 = [2.609374450746104   1.5832251296691495; 1.5832251296691495  8.482701064764527];
%sigma2 = [8.121011940540363 -1.4788051463562963; -1.4788051463562963  5.774691652901143];


% GAUSSIAN 1
%x1 = -4:.2:16; x2 = -2:.2:10;
x1 = -8:.2:8; x2 = -4:.2:4;
[X1,X2] = meshgrid(x1,x2);
F = mvnpdf([X1(:) X2(:)],mu1,sigma1);
F = reshape(F,length(x2),length(x1));

% GAUSSIAN 2
%x1b = -4:.2:16; x2b = -2:.2:10;
x1b = -8:.2:8; x2b = -4:.2:4;
[X1b,X2b] = meshgrid(x1b,x2b);
F2 = mvnpdf([X1b(:) X2b(:)],mu2,sigma2);
F2 = reshape(F2,length(x2b),length(x1b));

% figure %plot 2D gaussian 1
% surf(x1,x2,F);
% caxis([min(F(:))-.5*range(F(:)),max(F(:))]);
% axis([-4 4 -4 4 0 .4])
% xlabel('x1'); ylabel('x2'); zlabel('Probability Density');

% figure %plot 2D gaussian 2
% surf(x1b,x2b,F2);
% caxis([min(F2(:))-.5*range(F2(:)),max(F2(:))]);
% axis([-4 4 -4 4 0 .4])
% xlabel('x1'); ylabel('x2'); zlabel('Probability Density');


% CONTOUR PLOT
figure %first gaussian
plot(x_data,y_data,'.k');
title('Result of GMM in Spark');
hold on;
mvncdf([0 0],[1 1],mu1,sigma1);
v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c1a,h1a]=contour(x1,x2,F,v1);
set(h1a,'color','b');
%line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
xlabel('x'); ylabel('y');
hold on;
mvncdf([0 0],[1 1],mu2,sigma2);
v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c2a,h2a] = contour(x1b,x2b,F2,v2);
set(h2a,'color','g');
line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');


%figure %plot 2
%mvncdf([0 0],[1 1],mu2,sigma2);
%contour(x1b,x2b,F2,[.0001 .001 .01 .05:.1:.95 .99 .999 .9999]);
%xlabel('x'); ylabel('y');
%line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');

% %mixed plot
% figure
% plot(x_data,y_data,'.k');
% hold on;
% [c1,h1]=contour(x1,x2,F,v1);
% set(h1,'color',[0.6,0,0]);
% %line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
% xlabel('x'); ylabel('y');
% hold on;
% mvncdf([0 0],[1 1],mu2,sigma2);
% v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c2,h2] = contour(x1b,x2b,F2,v2);
% set(h2,'color',[0,0.6, 0.6]);
% %line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','w');

%% FLINK -1
% 
% 
% 
% 
% mu1 = [-0.04019677850522922 0.04493336696784432];
% sigma1 = [4.035428563347554 -1.1676306036510538; -1.1676306036510538 0.8185250656354032 ];
% mu2 = [0.04595001892235097 -0.016234454960378883];
% sigma2 = [7.445660600559506 3.1182260017360903; 3.1182260017360903 1.427609364769397 ];
% 
% 
% % GAUSSIAN 1
% x1 = -8:.2:8; x2 = -4:.2:4;
% %x1 = -4:.2:16; x2 = -2:.2:10;
% [X1,X2] = meshgrid(x1,x2);
% F = mvnpdf([X1(:) X2(:)],mu1,sigma1);
% F = reshape(F,length(x2),length(x1));
% 
% % GAUSSIAN 2
% x1b = -8:.2:8; x2b = -4:.2:4;
% %x1b = -4:.2:16; x2b = -2:.2:10;
% [X1b,X2b] = meshgrid(x1b,x2b);
% F2 = mvnpdf([X1b(:) X2b(:)],mu2,sigma2);
% F2 = reshape(F2,length(x2b),length(x1b));
% 
% % CONTOUR PLOT
% figure
% title 'first gaussian'
% mvncdf([0 0],[1 1],mu1,sigma1);
% v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c1a,h1a]=contour(x1,x2,F,v1);
% set(h1a,'color','g');
% %line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
% xlabel('x'); ylabel('y');
% hold on;
% mvncdf([0 0],[1 1],mu2,sigma2);
% v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c2a,h2a] = contour(x1b,x2b,F2,v2);
% set(h2a,'color','r');
% line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');
% 
% %%
% %%%  FLINK 1
% mu1 = [0.13249740792418727 0.6319899831634804 ];
% sigma1 = [13.580854172348566 -2.9378578720214077; -2.9378578720214077 2.3422583647593047 ];
% mu2 = [-0.03603754835125305 -0.03491879238153323 ];
% sigma2 = [3.905246290551963 0.15002971548940927; 0.15002971548940927 0.7724018100137653 ];
% 
% 
% % GAUSSIAN 1
% x1 = -8:.2:8; x2 = -4:.2:4;
% %x1 = -4:.2:16; x2 = -2:.2:10;
% [X1,X2] = meshgrid(x1,x2);
% F = mvnpdf([X1(:) X2(:)],mu1,sigma1);
% F = reshape(F,length(x2),length(x1));
% 
% % GAUSSIAN 2
% x1b = -8:.2:8; x2b = -4:.2:4;
% %x1b = -4:.2:16; x2b = -2:.2:10;
% [X1b,X2b] = meshgrid(x1b,x2b);
% F2 = mvnpdf([X1b(:) X2b(:)],mu2,sigma2);
% F2 = reshape(F2,length(x2b),length(x1b));
% 
% % CONTOUR PLOT
% figure %first gaussian
% mvncdf([0 0],[1 1],mu1,sigma1);
% v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c1a,h1a]=contour(x1,x2,F,v1);
% set(h1a,'color','m');
% %line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
% xlabel('x'); ylabel('y');
% hold on;
% mvncdf([0 0],[1 1],mu2,sigma2);
% v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c2a,h2a] = contour(x1b,x2b,F2,v2);
% set(h2a,'color','r');
% line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');
% 
% %% FLINK 2
% 
% weight1 = 0.480423;
% mu1 = [-0.1007464731064028 0.04138483143540518];
% sigma1 = [4.693632916317972 -1.88433672973958; -1.88433672973958 0.966824397310575 ];
% weight2 = 0.519577;
% mu2 = [0.08003135941661305 0.016659693121712102];
% sigma2 = [5.032008671420195 1.997957799074057; 1.997957799074057 0.9626676158814729 ];
% 
% % GAUSSIAN 1
% x1 = -8:.2:8; x2 = -4:.2:4;
% %x1 = -4:.2:16; x2 = -2:.2:10;
% [X1,X2] = meshgrid(x1,x2);
% F = mvnpdf([X1(:) X2(:)],mu1,sigma1);
% F = reshape(F,length(x2),length(x1));
% 
% % GAUSSIAN 2
% x1b = -8:.2:8; x2b = -4:.2:4;
% %x1b = -4:.2:16; x2b = -2:.2:10;
% [X1b,X2b] = meshgrid(x1b,x2b);
% F2 = mvnpdf([X1b(:) X2b(:)],mu2,sigma2);
% F2 = reshape(F2,length(x2b),length(x1b));
% 
% % CONTOUR PLOT
% figure %first gaussian
% mvncdf([0 0],[1 1],mu1,sigma1);
% v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c1a,h1a]=contour(x1,x2,F,v1);
% set(h1a,'color','k');
% %line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
% xlabel('x'); ylabel('y');
% hold on;
% mvncdf([0 0],[1 1],mu2,sigma2);
% v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c2a,h2a] = contour(x1b,x2b,F2,v2);
% set(h2a,'color',[0,0.6, 0.6]);
% line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');
% % 
% %% FLINK 3
% 
% weight1 = 0.480423;
% mu1 = [-0.09501633586223537 0.038893807526555614];
% sigma1 = [4.56208061195279 -1.7962482909982556; -1.7962482909982556 0.9376265052896335];
% weight2 = 0.519577;
% mu2 = [0.08290909709948775 0.018584659328739005];
% sigma2 = [5.231444806660202 2.1067208501457912; 2.1067208501457912 1.0021929111067072 ];
% 
% % GAUSSIAN 1
% x1 = -8:.2:8; x2 = -4:.2:4;
% %x1 = -4:.2:16; x2 = -2:.2:10;
% [X1,X2] = meshgrid(x1,x2);
% F = mvnpdf([X1(:) X2(:)],mu1,sigma1);
% F = reshape(F,length(x2),length(x1));
% 
% % GAUSSIAN 2
% x1b = -8:.2:8; x2b = -4:.2:4;
% %x1b = -4:.2:16; x2b = -2:.2:10;
% [X1b,X2b] = meshgrid(x1b,x2b);
% F2 = mvnpdf([X1b(:) X2b(:)],mu2,sigma2);
% F2 = reshape(F2,length(x2b),length(x1b));
% 
% % CONTOUR PLOT
% figure %first gaussian
% mvncdf([0 0],[1 1],mu1,sigma1);
% v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c1a,h1a]=contour(x1,x2,F,v1);
% set(h1a,'color','c');
% %line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
% xlabel('x'); ylabel('y');
% hold on;
% mvncdf([0 0],[1 1],mu2,sigma2);
% v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c2a,h2a] = contour(x1b,x2b,F2,v2);
% set(h2a,'color',[0,0.6, 0.6]);
% line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');
% 
% % %% 1D test data
% % mean1=3.4253928669661167;
% % sigma1=1.9949119641804565;
% % x=-10:0.01:20;
% % fx1=1/sqrt(2*pi)/sigma1*exp(-(x-mean1).^2/2/sigma1/sigma1);
% % 
% % mean2=7.549777184570953;
% % sigma2= 0.8180433810845744;
% % fx2=1/sqrt(2*pi)/sigma2*exp(-(x-mean2).^2/2/sigma2/sigma2);
% % 
% % figure
% % plot(x,fx1);
% % hold on;
% % plot(x,fx2);
% 
% 
% %% FLINK 4
% 
% weight1 = 0.480423;
% mu1 = [-0.1081053977760752 0.04412444196447256];
% sigma1 = [5.00668295723041 -2.058956357494829; -2.058956357494829 1.031370591694933 ];
% weight2 = 0.519577;
% mu2 = [0.07112432835141763 0.016102891096410485];
% sigma2 = [4.680723966873619 1.8239614855495347; 1.8239614855495347 0.8968479173138779];
% 
% % GAUSSIAN 1
% x1 = -8:.2:8; x2 = -4:.2:4;
% %x1 = -4:.2:16; x2 = -2:.2:10;
% [X1,X2] = meshgrid(x1,x2);
% F = mvnpdf([X1(:) X2(:)],mu1,sigma1);
% F = reshape(F,length(x2),length(x1));
% 
% % GAUSSIAN 2
% x1b = -8:.2:8; x2b = -4:.2:4;
% %x1b = -4:.2:16; x2b = -2:.2:10;
% [X1b,X2b] = meshgrid(x1b,x2b);
% F2 = mvnpdf([X1b(:) X2b(:)],mu2,sigma2);
% F2 = reshape(F2,length(x2b),length(x1b));
% 
% % CONTOUR PLOT
% figure %first gaussian
% mvncdf([0 0],[1 1],mu1,sigma1);
% v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c1a,h1a]=contour(x1,x2,F,v1);
% set(h1a,'color','k');
% %line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
% xlabel('x'); ylabel('y');
% hold on;
% mvncdf([0 0],[1 1],mu2,sigma2);
% v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
% [c2a,h2a] = contour(x1b,x2b,F2,v2);
% set(h2a,'color','c');
% line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');



%% FLINK FINAL

weight1 = 0.480423;
mu1 = [-0.10504540545596944 0.04302887675903197];
sigma1 = [4.922733470137361 -2.0151500731531637; -2.0151500731531637 1.0146136345598653];
weight2 = 0.519577;
mu2 = [0.07204127542852905 0.016620850184660235];
sigma2 = [4.763462685580626 1.867741149095852; 1.867741149095852 0.9117006288849978];

% GAUSSIAN 1
x1 = -8:.2:8; x2 = -4:.2:4;
%x1 = -4:.2:16; x2 = -2:.2:10;
[X1,X2] = meshgrid(x1,x2);
F = mvnpdf([X1(:) X2(:)],mu1,sigma1);
F = reshape(F,length(x2),length(x1));

% GAUSSIAN 2
x1b = -8:.2:8; x2b = -4:.2:4;
%x1b = -4:.2:16; x2b = -2:.2:10;
[X1b,X2b] = meshgrid(x1b,x2b);
F2 = mvnpdf([X1b(:) X2b(:)],mu2,sigma2);
F2 = reshape(F2,length(x2b),length(x1b));

% CONTOUR PLOT
figure %first gaussian
plot(x_data,y_data,'.k');
hold on;
mvncdf([0 0],[1 1],mu1,sigma1);
v1=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c1a,h1a]=contour(x1,x2,F,v1);
set(h1a,'color','g');
%line([0 0 1 1 0],[1 0 0 1 1],'linestyle','--','color','k');
xlabel('x'); ylabel('y');
hold on;
mvncdf([0 0],[1 1],mu2,sigma2);
v2=[.0001 .001 .01 .05:.1:.95 .99 .999 .9999];
[c2a,h2a] = contour(x1b,x2b,F2,v2);
set(h2a,'color','b');
line([0 0 1 1 0],[1 0 0 1 1],'linestyle',':','color','r');
